package com.example.matjang_aos

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.matjang_aos.databinding.ActivitySignInViewBinding
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient

class SignInActivity : AppCompatActivity() {

    private val TAG = "SignInActivity"
    private lateinit var binding: ActivitySignInViewBinding
    private lateinit var signInPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        signInPrefs = getSharedPreferences("signIn", Context.MODE_PRIVATE)

        checkBrokenUserPrefs() // 잘못 저장된 유저 정보 정리
        autoLoginIfPossible() // 자동 로그인 처리
        setupSignInButton()   // 버튼 초기화
    }

    private fun checkBrokenUserPrefs() {
        val brokenPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val brokenEmail = brokenPref.getString("current_user", "") ?: ""
        if (brokenEmail.contains("\"email\":\"\"")) {
            brokenPref.edit().clear().apply()
            Log.d(TAG, "잘못 저장된 유저 정보 삭제됨")
        }
    }

    private fun autoLoginIfPossible() {
        val token = signInPrefs.getString("token", null)
        val email = signInPrefs.getString("email", null)

        if (token != null && email != null) {
            UserManager.init(this, email) {
                goToMainMap()
            }
        }
    }

    private fun setupSignInButton() {
        binding.signInKakao.setOnClickListener {
            signInKakao()
        }
    }

    private fun signInKakao() {
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e(TAG, "카카오계정으로 로그인 실패", error)
            } else if (token != null) {
                Log.i(TAG, "카카오계정으로 로그인 성공 ${token.accessToken}")
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                handleLoginResult(token, error)
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
                handleLoginResult(token, error)
            }
        }
    }

    private fun handleLoginResult(token: OAuthToken?, error: Throwable?) {
        if (error != null) {
            Log.e(TAG, "로그인 실패", error)
            if (error is ClientError && error.reason == ClientErrorCause.Cancelled) return
        }

        if (token == null) return

        UserApiClient.instance.me { user, error ->
            if (error != null || user == null) {
                Log.e(TAG, "사용자 정보 요청 실패", error)
                return@me
            }

            val email = user.kakaoAccount?.email
            if (email.isNullOrBlank()) {
                Log.e(TAG, "이메일 정보 없음 (null or blank)")
                return@me
            }

            signInPrefs.edit().apply {
                putString("token", token.accessToken)
                putString("email", email)
                apply()
            }

            UserManager.init(this, email) {
                goToMainMap()
            }
        }
    }

    private fun goToMainMap() {
        startActivity(Intent(this, MainMapActivity::class.java))
        finish()
    }
}
