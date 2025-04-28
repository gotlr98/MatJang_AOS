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
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import java.util.Locale

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoApiService {

    // 카테고리로 장소 검색
    @GET("v2/local/search/category.json")
    fun searchByCategory(
        @Header("Authorization") apiKey: String,  // REST API 키를 헤더에 포함
        @Query("category_group_code") category: String,  // 카테고리 코드
        @Query("x") longitude: Double,  // 경도
        @Query("y") latitude: Double,   // 위도
        @Query("radius") radius: Int    // 검색 반경 (미터)
    ): Call<CategorySearchResponse>
}


class MainMap : AppCompatActivity() {


    private lateinit var mapView: MapView

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    val retrofit = Retrofit.Builder()
        .baseUrl("https://dapi.kakao.com/")  // 카카오 API의 베이스 URL
        .addConverterFactory(GsonConverterFactory.create())  // Gson을 이용해 JSON을 객체로 변환
        .build()

    val apiService = retrofit.create(KakaoApiService::class.java)


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

//        val btnPlaceMarker = findViewById<Button>(R.id.btn_place_marker)

        // KakaoMapReadyCallback 객체 생성
        val readyCallback = object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                // 지도 준비 완료 시 호출
                Log.d("KakaoMap", "Map is ready!")

                val labelManager = map.labelManager
                val layer = labelManager?.getLayer()
                layer?.removeAll()

                map.setOnCameraMoveEndListener { kakaoMap, position, gestureType ->
                    // position 파라미터를 이용해서 원하는 작업을 수행



                    Log.d("camera stop", position.toString())
                    Log.d("cur Camera Position", "latitude: ${position.position.latitude} " +
                            "longitude${position.position.longitude}")

                    val curLatLng = LatLng.from(position.position.latitude, position.position.longitude)


                    val styles = labelManager
                        ?.addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.marker)))

                    val options = LabelOptions.from(LatLng.from(curLatLng))
                        .setStyles(styles)


                    val label = layer?.addLabel(options);

                    searchPlacesByCategory(curLatLng)
                }

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

    fun searchPlacesByCategory(latlng: LatLng) {
        val apiKey = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}"  // 카카오 REST API 키 (실제 키로 대체하세요)
        val categoryCode = "FD6"  // 음식점 카테고리 코드
        val longitude = latlng.longitude  // 경도 (예시: 서울)
        val latitude = latlng.latitude   // 위도 (예시: 서울)
        val radius = 1000        // 검색 반경 1km

        apiService.searchByCategory(apiKey, categoryCode, longitude, latitude, radius)
            .enqueue(object : Callback<CategorySearchResponse> {
                override fun onResponse(
                    call: Call<CategorySearchResponse>,
                    response: Response<CategorySearchResponse>
                ) {
                    if (response.isSuccessful) {
                        val places = response.body()?.documents
                        places?.forEach {
                            println("Place Name: ${it.place_name}")
                            println("Category: ${it.category_name}")
                            println("Address: ${it.address_name}")
                            println("Phone: ${it.phone}")
                            println("Latitude: ${it.latitude}, Longitude: ${it.longitude}")
                        }
                    } else {
                        println("Request failed with code: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<CategorySearchResponse>, t: Throwable) {
                    println("Error: ${t.message}")
                }
            })
    }






}