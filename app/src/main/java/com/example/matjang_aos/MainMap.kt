package com.example.matjang_aos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView


class MainMap : AppCompatActivity() {


    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_map) // ← 너의 레이아웃 이름으로 변경

        KakaoMapSdk.init(this, BuildConfig.NATIVE_APP_KEY)

        mapView = findViewById(R.id.map_view)

        // MapLifeCycleCallback 객체 생성
        val lifeCycleCallback = object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                // 지도 API가 정상적으로 종료될 때 호출
                Log.d("KakaoMap", "Map destroyed")
            }

            override fun onMapError(error: Exception) {
                // 지도 API 에러 발생 시 호출
                Log.e("KakaoMap", "Map error: ${error.message}")
            }
        }

        // KakaoMapReadyCallback 객체 생성
        val readyCallback = object : KakaoMapReadyCallback() {
            override fun onMapReady(kakaoMap: KakaoMap) {
                // 지도 준비 완료 시 호출
                Log.d("KakaoMap", "Map is ready!")
                // 여기에 kakaoMap 조작 코드 작성 가능
            }
        }

        // 지도 시작
        mapView.start(lifeCycleCallback, readyCallback)
    }

    override fun onResume() {
        super.onResume()
        mapView.resume() // 지도 resume
    }

    override fun onPause() {
        super.onPause()
        mapView.pause() // 지도 pause
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.pause() // 지도 종료
    }






}