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
import android.nfc.Tag
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignInView : AppCompatActivity() {

    val db = Firebase.firestore
    val pref = getSharedPreferences("signIn", Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = pref.getString("token", null)

        if (token != null) {
            // 이미 로그인된 상태 -> 사용자 정보 요청
            UserApiClient.instance.me { user, error ->
                if (error != null) {
                    Log.e(TAG, "자동 로그인 실패", error)
                } else if (user != null) {
                    val user_default = UserModel(
                        email = user.kakaoAccount?.email,
                        type = Type.Kakao
                    )

                    val intent = Intent(this, MainMap::class.java)
                    intent.putExtra("user", user_default)
                    startActivity(intent)
                    finish() // 현재 로그인 화면 종료
                }
            }
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val signInBtn = findViewById<ImageButton>(R.id.signInKakao) as ImageButton
        signInBtn.setOnClickListener{
            Log.d(TAG, "button click")
            signInKakao()
        }
    }

    fun signInKakao() {

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e(TAG, "카카오계정으로 로그인 실패", error)
            } else if (token != null) {
                Log.i(TAG, "카카오계정으로 로그인 성공 ${token.accessToken}")
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            // 카카오톡 로그인
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                // 로그인 실패 부분
                if (error != null) {
                    Log.e(TAG, "로그인 실패 $error")
                    // 사용자가 취소
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        return@loginWithKakaoTalk
                    }
                    // 다른 오류
                    else {
                        UserApiClient.instance.loginWithKakaoAccount(
                            this,
                            callback = callback


                        ) // 카카오 이메일 로그인

                        UserApiClient.instance.me { user, error ->
                            if (error != null) {
                                Log.e(TAG, "사용자 정보 요청 실패", error)
                            } else if (user != null) {

                                val user_default =
                                    UserModel(email = user.kakaoAccount?.email, type = Type.Kakao)


                                db.collection("users").document(
                                    "${user.kakaoAccount?.email}&Kakao"
                                ).set({})

                                val intent = Intent(this, MainMap::class.java)
                                pref.edit().putString("token", token?.accessToken).apply()
                                pref.edit().putString("email", user.kakaoAccount?.email).apply()
                                startActivity(intent)
                            }
                        }

                    }
                }
                // 로그인 성공 부분
                else if (token != null) {
                    Log.e(TAG, "로그인 성공 ${token.accessToken}")
                    UserApiClient.instance.me { user, error ->
                        if (error != null) {
                            Log.e(TAG, "사용자 정보 요청 실패", error)
                        } else if (user != null) {
                            val user_default =
                                UserModel(email = user.kakaoAccount?.email, type = Type.Kakao)


                            db.collection("users").document(
                                "${user.kakaoAccount?.email}&Kakao"
                            ).set({})

                            val intent = Intent(this, MainMap::class.java)
                            pref.edit().putString("token", token?.accessToken).apply()
                            pref.edit().putString("email", user.kakaoAccount?.email).apply()
                            startActivity(intent)
                        }
                    }

                }
            }
        }
        else {
            UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
                if (error != null) {
                    Log.e(TAG, "카카오계정으로 로그인 실패", error)
                } else if (token != null) {
                    Log.i(TAG, "카카오계정으로 로그인 성공 ${token.accessToken}")

                    // SharedPreferences에 토큰 저장
                    val pref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    pref.edit().putString("token", token.accessToken).apply()

                    // 사용자 정보 요청
                    UserApiClient.instance.me { user, error ->
                        if (error != null) {
                            Log.e(TAG, "사용자 정보 요청 실패", error)
                        } else if (user != null) {
                            val user_default = UserModel(
                                email = user.kakaoAccount?.email,
                                type = Type.Kakao
                            )

                            pref.edit().putString("email", user.kakaoAccount?.email).apply()

                            db.collection("users").document(
                                "${user.kakaoAccount?.email}&Kakao"
                            ).set({})

                            val intent = Intent(this, MainMap::class.java)
                            intent.putExtra("user", user_default)
                            startActivity(intent)
                        }
                    }
                }
            }
        }


    }
}