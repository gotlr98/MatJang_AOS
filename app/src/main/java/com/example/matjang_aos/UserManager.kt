package com.example.matjang_aos

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson

object UserManager {
    private const val PREF_NAME = "user_prefs"
    private const val KEY_USER = "current_user"

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

    fun isLoggedIn(): Boolean {
        return _currentUser != null
    }

    fun saveUserToPrefs(context: Context) {
        currentUser?.let { user ->
            val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            val userJson = Gson().toJson(user)
            editor.putString(KEY_USER, userJson)
            editor.apply()
        }
    }

    fun loadUserFromPrefs(context: Context) {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val userJson = sharedPref.getString(KEY_USER, null)

        userJson?.let {
            val loadedUser = Gson().fromJson(it, UserModel::class.java)

            // ✅ 이메일이 비어 있으면 무시 (초기화하지 않음)
            if (loadedUser.email.isNullOrBlank()) {
                Log.w("UserManager", "저장된 사용자 이메일이 비어있음. 초기화하지 않음.")
                _currentUser = null
                return
            }

            _currentUser = loadedUser
        }

        Log.d("UserManager", "loaded user json: $userJson")
    }


    /**
     * Firebase에서 유저 정보 초기화 및 SharedPreferences 저장
     */
    fun init(context: Context, email: String, onComplete: (UserModel) -> Unit) {
        val db = Firebase.firestore
        val docId = "$email&Kakao"

        db.collection("users").document(docId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val user = doc.toObject(UserModel::class.java)!!
                    _currentUser = user
                    saveUserToPrefs(context)
                    onComplete(user)
                    Log.d("UserManager", "saved email to prefs: ${user.email}")

                } else {
                    val newUser = UserModel(email = email, type = Type.Kakao, reviews = emptyList())
                    db.collection("users").document(docId).set(newUser)
                        .addOnSuccessListener {
                            // 하위 컬렉션은 이후 bookmark, review 액션 시점에 생성
                            _currentUser = newUser
                            saveUserToPrefs(context)
                            onComplete(newUser)

                        }
                        .addOnFailureListener { e ->
                            Log.e("UserManager", "새 유저 저장 실패: ${e.message}")
                            _currentUser = newUser
                            saveUserToPrefs(context)
                            onComplete(newUser)

                        }
                }
            }
            .addOnFailureListener {
                Log.e("UserManager", "유저 불러오기 실패: ${it.message}")
                val fallbackUser = UserModel(email = email, type = Type.Kakao, reviews = emptyList())
                _currentUser = fallbackUser
                saveUserToPrefs(context)
                onComplete(fallbackUser)

            }
    }
}