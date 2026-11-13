plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.etashe.tunnelvpn"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.etashe.tunnelvpn"
        // The real Xray-core engine (app/libs/libv2ray.aar, built by CI from
        // github.com/2dust/AndroidLibXrayLite with `gomobile bind -androidapi 24`)
        // declares minSdk 24 in its own manifest, and the manifest merger fails the
        // build if our minSdk is lower ("the library might be using APIs not
        // available" below 24). v2rayNG and every other Xray-core-based VPN app
        // carry the same floor for the same reason, so this isn't a choice we can
        // avoid while shipping the real engine — Android 7.0 (API 24) covers the
        // overwhelming majority of active devices.
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Real Xray-core engine (VLESS/VMess/Trojan/Shadowsocks). Built fresh from
    // github.com/2dust/AndroidLibXrayLite source by the CI workflow (gomobile bind)
    // and dropped here as app/libs/libv2ray.aar before this build runs — it is not
    // committed to the repo. A local `./gradlew` run outside CI will not have this
    // file and Kotlin compilation of the core.xray package will fail; that is expected,
    // this project is only ever built through GitHub Actions.
    implementation(files("libs/libv2ray.aar"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation("junit:junit:4.13.2")
    // Real org.json implementation for JVM unit tests — Android's android.jar only
    // ships stub bodies for org.json.* that throw at runtime unless this is on
    // the test classpath explicitly.
    testImplementation("org.json:json:20231013")
}
