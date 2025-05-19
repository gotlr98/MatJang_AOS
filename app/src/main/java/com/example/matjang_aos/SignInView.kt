package com.example.matjang_aos

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import android.widget.Button
import android.widget.ImageButton
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.nfc.Tag
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignInView : AppCompatActivity() {

    private val TAG = "SignInView"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_view)

        val brokenPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val brokenEmail = brokenPref.getString("current_user", "") ?: ""
        if (brokenEmail.contains("\"email\":\"\"")) {
            brokenPref.edit().clear().apply()
            Log.d("SignInView", "잘못 저장된 유저 정보 삭제됨")
        }

        val pref = getSharedPreferences("signIn", Context.MODE_PRIVATE)
        val token = pref.getString("token", null)
        val email = pref.getString("email", null)

        // ✅ 자동 로그인 처리
        if (token != null && email != null) {
            UserManager.init(this, email) {
                goToMainMap()
            }
        }

        val signInBtn = findViewById<ImageButton>(R.id.signInKakao)
        signInBtn.setOnClickListener {
            signInKakao()
        }
    }

    private fun signInKakao() {
        val pref = getSharedPreferences("signIn", Context.MODE_PRIVATE)

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e(TAG, "카카오계정으로 로그인 실패", error)
            } else if (token != null) {
                Log.i(TAG, "카카오계정으로 로그인 성공 ${token.accessToken}")
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            // ✅ 카카오톡 앱 로그인
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                handleLoginResult(token, error, pref)
            }
        } else {
            // ✅ 카카오 계정(웹뷰) 로그인
            UserApiClient.instance.loginWithKakaoAccount(this, callback = { token, error ->
                handleLoginResult(token, error, pref)
            })
        }
    }

    private fun handleLoginResult(token: OAuthToken?, error: Throwable?, pref: SharedPreferences) {
        if (error != null) {
            Log.e("SignInView", "로그인 실패 $error")
            if (error is ClientError && error.reason == ClientErrorCause.Cancelled) return
        }

        if (token != null) {
            UserApiClient.instance.me { user, error ->
                if (error != null || user == null) {
                    Log.e("SignInView", "사용자 정보 요청 실패", error)
                    return@me
                }

                val email = user.kakaoAccount?.email
                if (email.isNullOrBlank()) {
                    Log.e("SignInView", "이메일 정보 없음 (null or blank)")
                    return@me
                }

                // ✅ 유효한 이메일일 때만 저장 및 초기화
                pref.edit().putString("token", token.accessToken).apply()
                pref.edit().putString("email", email).apply()

                UserManager.init(this, email) {
                    goToMainMap()
                }
            }
        }
    }


    private fun goToMainMap() {
        val intent = Intent(this, MainMap::class.java)
        startActivity(intent)
        finish()
    }
}