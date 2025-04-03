package com.example.matjang_aos

import android.app.Application
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.Utility
import com.umc.anddeul.R

class GlobalApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        var keyHash = Utility.getKeyHash(this)
        Log.d("키 확인", keyHash)
        KakaoSdk.init(this, getString(R.string.kakao_app_key))
    }
}