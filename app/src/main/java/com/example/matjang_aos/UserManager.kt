package com.example.matjang_aos

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson

object UserManager {
    private const val PREF_NAME = "user_prefs"
    private const val KEY_USER = "current_user"
    private val db by lazy { Firebase.firestore }

    private var _currentUser: UserModel? = null
    var currentUser: UserModel?
        get() = _currentUser
        private set(value) {
            _currentUser = value
        }

    fun login(user: UserModel) {
        _currentUser = user
    }

    fun logout() {
        _currentUser = null
    }

    fun isLoggedIn(): Boolean = _currentUser != null

    fun isGuest(): Boolean {
        val result = currentUser?.type == Type.Guest
        Log.d("UserManager", "isGuest() called: ${currentUser?.email}, type=${currentUser?.type}, result=$result")
        return currentUser?.type == Type.Guest
    }


    fun saveUserToPrefs(context: Context) {
        currentUser?.let { user ->
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_USER, Gson().toJson(user))
                .apply()
        }
    }

    fun loadUserFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val userJson = prefs.getString(KEY_USER, null)

        Log.d("UserManager", "Raw user JSON from prefs: $userJson")

        if (!userJson.isNullOrBlank()) {
            try {
                val user = Gson().fromJson(userJson, UserModel::class.java)
                if (!user.email.isNullOrBlank()) {
                    _currentUser = user
                    Log.d("UserManager", "loadUserFromPrefs: user loaded - email=${user.email}, type=${user.type}")
                }
            } catch (e: Exception) {
                Log.e("UserManager", "loadUserFromPrefs 실패: ${e.message}")
            }
        } else {
            Log.w("UserManager", "userJson이 null 또는 blank임")
        }
    }


    fun init(context: Context, email: String, onComplete: (UserModel) -> Unit) {
        val docId = "$email&kakao"

        db.collection("users").document(docId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    doc.toObject(UserModel::class.java)?.let {
                        completeLogin(context, it, onComplete)
                    }
                } else {
                    createNewUser(context, email, onComplete)
                }
            }
            .addOnFailureListener {
                Log.e("UserManager", "유저 불러오기 실패: ${it.message}")
                val fallbackUser = UserModel(email = email, type = Type.kakao, reviews = emptyList())
                completeLogin(context, fallbackUser, onComplete)
            }
    }

    private fun createNewUser(context: Context, email: String, onComplete: (UserModel) -> Unit) {
        val newUser = UserModel(email = email, type = Type.kakao, reviews = emptyList())
        val docId = "$email&kakao"

        db.collection("users").document(docId).set(newUser)
            .addOnSuccessListener {
                completeLogin(context, newUser, onComplete)
            }
            .addOnFailureListener { e ->
                Log.e("UserManager", "새 유저 저장 실패: ${e.message}")
                completeLogin(context, newUser, onComplete)
            }
    }

    private fun completeLogin(context: Context, user: UserModel, onComplete: (UserModel) -> Unit) {
        _currentUser = user
        saveUserToPrefs(context)
        onComplete(user)
        Log.d("UserManager", "User initialized: ${user.email}")
    }
}
