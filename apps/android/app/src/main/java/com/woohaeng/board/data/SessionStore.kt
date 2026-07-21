package com.woohaeng.board.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("session")

class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("token")
    private val nameKey = stringPreferencesKey("name")
    private val usernameKey = stringPreferencesKey("username")
    private val roleKey = stringPreferencesKey("role")

    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val userName: Flow<String?> = context.dataStore.data.map { it[nameKey] }

    suspend fun save(token: String, user: UserDto) {
        context.dataStore.edit {
            it[tokenKey] = token
            it[nameKey] = user.name
            it[usernameKey] = user.username
            it[roleKey] = user.role
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
