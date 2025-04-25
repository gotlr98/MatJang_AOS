package com.example.matjang_aos

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.label.Label

import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelManager
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelTextBuilder


class MainMap : AppCompatActivity() {


    private lateinit var mapView: MapView

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private var labelManager: LabelManager? = null
    private var marker: Label? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_map) //


        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        val menuButton = findViewById<ImageButton>(R.id.menu_button)
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val pref by lazy { getSharedPreferences("signIn", Context.MODE_PRIVATE) }

        val headerView = navigationView.getHeaderView(0)
        val emailText = headerView.findViewById<TextView>(R.id.email_text)
        val email = pref.getString("email", "알 수 없음")
        emailText.text = email

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_settings -> {
                    Toast.makeText(this, "설정 클릭됨", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_logout -> {
                    Toast.makeText(this, "로그아웃 클릭됨", Toast.LENGTH_SHORT).show()
                    // 로그아웃 로직 추가 가능
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

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

        val btnPlaceMarker = findViewById<Button>(R.id.btn_place_marker)

        // KakaoMapReadyCallback 객체 생성
        val readyCallback = object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                // 지도 준비 완료 시 호출
                Log.d("KakaoMap", "Map is ready!")


                labelManager = mapView.labelManager

                // 카메라 이동이 끝났을 때 이벤트

                map.setOnCameraMoveEndListener { map, cameraPosition, gestureType ->
                    val center = cameraPosition.position
                    placeMarker(center)
                }
            }
        }

        // 지도 시작
        mapView.start(lifeCycleCallback, readyCallback)
    }

    private fun placeMarker(position: LatLng) {
        labelManager?.clear() // 기존 마커 제거

        val markerOptions = LabelOptions.from(position)
            .setTexts(LabelText.builder("여기예요").build())
            .setTag("centerMarker")

        marker = labelManager?.addLabel(markerOptions)

        marker?.setOnLabelClickListener { _, _ ->
            Toast.makeText(this, "위도: ${position.latitude}, 경도: ${position.longitude}", Toast.LENGTH_SHORT).show()
            true
        }
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