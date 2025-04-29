package com.example.matjang_aos

import android.content.Context
import com.google.gson.Gson

object UserManager {
    private var _currentUser: UserModel? = null
    var currentUser: UserModel? = null
        get() = _currentUser

    fun login(user: UserModel) {
        _currentUser = user
    }

    fun logout() {
        _currentUser = null
    }

    fun isLoggedIn(): Boolean {
        return _currentUser != null
    }

    fun saveUserToPrefs(context: Context) {
        currentUser?.let { user ->
            val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            val userJson = Gson().toJson(user)  // Gson 사용
            editor.putString("current_user", userJson)
            editor.apply()
        }
    }

    fun loadUserFromPrefs(context: Context) {
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userJson = sharedPref.getString("current_user", null)
        userJson?.let {
            currentUser = Gson().fromJson(it, UserModel::class.java)
        }
    }
}