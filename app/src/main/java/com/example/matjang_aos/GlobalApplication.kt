package com.example.matjang_aos

import android.app.Application
import android.util.Log
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        KakaoSdk.init(this, BuildConfig.NATIVE_APP_KEY)
    }
}