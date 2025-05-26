package com.example.matjang_aos

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
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

    private lateinit var kakaoMap: KakaoMap

    private var mapMode: String = "BROWSE"

    val retrofit = Retrofit.Builder()
        .baseUrl("https://dapi.kakao.com/")  // 카카오 API의 베이스 URL
        .addConverterFactory(GsonConverterFactory.create())  // Gson을 이용해 JSON을 객체로 변환
        .build()

    val apiService = retrofit.create(KakaoApiService::class.java)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_map) //

        UserManager.loadUserFromPrefs(this)


        if (UserManager.currentUser == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        val menuButton = findViewById<ImageButton>(R.id.menu_button)
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val modeSpinner = findViewById<Spinner>(R.id.map_mode_spinner)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.map_modes, // res/values/strings.xml 에 정의된 배열
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modeSpinner.adapter = adapter
        modeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                mapMode = if (position == 0) "BROWSE" else "FIND_MATJIP"
                Log.d("MapMode", "Selected mode: $mapMode")

                if (::kakaoMap.isInitialized) {
                    val curLatLng = LatLng.from(
                        kakaoMap.cameraPosition!!.position.latitude,
                        kakaoMap.cameraPosition!!.position.longitude
                    )

                    val loadLayer = kakaoMap.labelManager?.getLodLayer()
                    loadLayer?.removeAll()

                    if (mapMode == "FIND_MATJIP") {
                        // 1️⃣ 맵 움직임 리스너 재등록
                        kakaoMap.setOnCameraMoveEndListener { _, position, _ ->
                            val latLng = LatLng.from(position.position.latitude, position.position.longitude)

                            val innerLoadLayer = kakaoMap.labelManager?.getLodLayer()
                            innerLoadLayer?.removeAll()

                            searchPlacesByCategory(latLng) { places ->
                                val labelStyles = LabelStyles.from(LabelStyle.from(R.drawable.marker))
                                val options = mutableListOf<LabelOptions>()

                                places?.forEach { place ->
                                    val p = LatLng.from(place.latitude, place.longitude)
                                    val labelOptions = LabelOptions.from(p)
                                        .setStyles(labelStyles)
                                        .setTag(place)
                                    options.add(labelOptions)
                                }

                                innerLoadLayer?.addLodLabels(options)
                            }
                        }

                        // 2️⃣ 초기 라벨 세팅도 해줌 (지도 안 움직이고도 보이게)
                        searchPlacesByCategory(curLatLng) { places ->
                            val labelStyles = LabelStyles.from(LabelStyle.from(R.drawable.marker))
                            val options = mutableListOf<LabelOptions>()

                            places?.forEach { place ->
                                val p = LatLng.from(place.latitude, place.longitude)
                                val labelOptions = LabelOptions.from(p)
                                    .setStyles(labelStyles)
                                    .setTag(place)
                                options.add(labelOptions)
                            }

                            loadLayer?.addLodLabels(options)
                        }
                    } else {
                        // 모드가 BROWSE면 리스너 제거
                        kakaoMap.setOnCameraMoveEndListener(null)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
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

                kakaoMap = map
                // 지도 준비 완료 시 호출
                Log.d("KakaoMap", "Map is ready!")

                val labelManager = kakaoMap.labelManager
                val layer = labelManager?.getLayer()
                layer?.removeAll()

                if(mapMode == "FIND_MATJIP"){
                    kakaoMap.setOnCameraMoveEndListener { map, position, gestureType ->
                        // position 파라미터를 이용해서 원하는 작업을 수행


                        val curLatLng = LatLng.from(position.position.latitude, position.position.longitude)

                        val labelStyles = LabelStyles.from(LabelStyle.from(R.drawable.marker))
                        var options = mutableListOf<LabelOptions>()


                        val load_layer = map.labelManager?.getLodLayer()
                        load_layer?.removeAll()

                        searchPlacesByCategory(curLatLng) { places ->
                            places?.forEach {place ->

                                Log.d("place_information", "${place.placeName} ${place.latitude.toString()} ${place.longitude.toString()}")

                                val tempLatLng = LatLng.from(place.latitude, place.longitude)
                                val labelOptoins = LabelOptions.from(tempLatLng).setStyles(labelStyles).setTag(place)
                                options.add(labelOptoins)
                            }
                            load_layer?.addLodLabels(options)

                        }

                        map.setOnLodLabelClickListener { kakaoMap, layer, label ->
                            val tag = label.tag
                            if (tag is Matjip) {
                                showPlaceDetailDialog(tag)

                            } else {
                                Log.e("LOD_CLICK", "Tag is not Matjip or is null: $tag")
                            }
                            true
                        }


                    }
                }



            }
        }

        // 지도 시작
        mapView.start(lifeCycleCallback, readyCallback)
    }

    fun showPlaceDetailDialog(place: Matjip) {
        val fragmentManager = supportFragmentManager
        val existingFragment = fragmentManager.findFragmentByTag("place_detail")

        if (existingFragment != null && existingFragment is PlaceDetailBottomSheetFragment) {
            if (existingFragment.isAdded) {
                fragmentManager.beginTransaction()
                    .remove(existingFragment)
                    .commitAllowingStateLoss()
            }
        }

        val bottomSheetFragment = PlaceDetailBottomSheetFragment(place)
        if (!bottomSheetFragment.isAdded) {
            bottomSheetFragment.show(fragmentManager, "place_detail")
        }
    }
    override fun onResume() {
        super.onResume()
        mapView.resume() // 지도 resume
        Log.d("map", "map resume")

    }

    override fun onPause() {
        super.onPause()
        mapView.pause() // 지도 pause
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.pause() // 지도 종료
    }

    fun searchPlacesByCategory(latlng: LatLng, onResult: (List<Matjip>?) -> Unit) {
        val apiKey = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}"
        val categoryCode = "FD6"
        val longitude = latlng.longitude
        val latitude = latlng.latitude
        val radius = 1000

        apiService.searchByCategory(apiKey, categoryCode, longitude, latitude, radius)
            .enqueue(object : Callback<CategorySearchResponse> {
                override fun onResponse(
                    call: Call<CategorySearchResponse>,
                    response: Response<CategorySearchResponse>
                ) {
                    if (response.isSuccessful) {
                        val places = response.body()?.documents
                        onResult(places)  // 결과를 콜백으로 넘김
                    } else {
                        println("Request failed with code: ${response.code()}")
                        onResult(null)  // 실패했으면 null
                    }
                }

                override fun onFailure(call: Call<CategorySearchResponse>, t: Throwable) {
                    println("Error: ${t.message}")
                    onResult(null)
                }
            })
    }

}

