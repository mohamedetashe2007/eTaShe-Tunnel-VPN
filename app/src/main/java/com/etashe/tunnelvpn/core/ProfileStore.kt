package com.etashe.tunnelvpn.core

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Local persistence for imported/saved config profiles.
 * Simple SharedPreferences + Gson JSON blob — no server round-trip needed
 * for profiles the user imported manually on-device.
 */
class ProfileStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("etashe_profiles", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listType = object : TypeToken<MutableList<ConfigProfile>>() {}.type

    fun getAll(): MutableList<ConfigProfile> {
        val raw = prefs.getString(KEY_PROFILES, null) ?: return mutableListOf()
        return try {
            gson.fromJson(raw, listType) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun add(profile: ConfigProfile) {
        val all = getAll()
        all.add(0, profile)
        save(all)
    }

    fun remove(id: String) {
        val all = getAll()
        all.removeAll { it.id == id }
        save(all)
    }

    fun update(profile: ConfigProfile) {
        val all = getAll()
        val idx = all.indexOfFirst { it.id == profile.id }
        if (idx >= 0) {
            all[idx] = profile
            save(all)
        }
    }

    private fun save(list: List<ConfigProfile>) {
        prefs.edit().putString(KEY_PROFILES, gson.toJson(list)).apply()
    }

    companion object {
        private const val KEY_PROFILES = "profiles_json"
    }
}
