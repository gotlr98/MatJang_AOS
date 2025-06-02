package com.example.matjang_aos

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.kakao.vectormap.*
import com.kakao.vectormap.label.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoApiService {
    @GET("v2/local/search/category.json")
    fun searchByCategory(
        @Header("Authorization") apiKey: String,
        @Query("category_group_code") category: String,
        @Query("x") longitude: Double,
        @Query("y") latitude: Double,
        @Query("radius") radius: Int
    ): Call<CategorySearchResponse>
}

class MainMap : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var kakaoMap: KakaoMap
    private var mapMode: String = "BROWSE"

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://dapi.kakao.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(KakaoApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_map)

        intent.getStringExtra("mapType")?.let {
            mapMode = it
            Log.d("MapMode", "Restored map mode from intent: $mapMode")
        }

        UserManager.loadUserFromPrefs(this)
        if (UserManager.currentUser == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupMap()
    }

    private fun setupUI() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        findViewById<ImageButton>(R.id.menu_button).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val modeSpinner = findViewById<Spinner>(R.id.map_mode_spinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.map_modes,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            modeSpinner.adapter = adapter
        }

        modeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                mapMode = if (position == 0) "BROWSE" else "FIND_MATJIP"
                Log.d("MapMode", "Selected mode: $mapMode")
                handleMapModeChange()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val email = getSharedPreferences("signIn", Context.MODE_PRIVATE).getString("email", "알 수 없음")
        navigationView.getHeaderView(0).findViewById<TextView>(R.id.email_text).text = email

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_settings -> Toast.makeText(this, "설정 클릭됨", Toast.LENGTH_SHORT).show()
                R.id.nav_logout -> Toast.makeText(this, "로그아웃 클릭됨", Toast.LENGTH_SHORT).show()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupMap() {
        KakaoMapSdk.init(this, BuildConfig.NATIVE_APP_KEY)
        mapView = findViewById(R.id.map_view)

        mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                Log.d("KakaoMap", "Map destroyed")
            }

            override fun onMapError(error: Exception) {
                Log.e("KakaoMap", "Map error: ${error.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                Log.d("KakaoMap", "Map is ready!")

                kakaoMap.labelManager?.getLayer()?.removeAll()
                if (mapMode == "FIND_MATJIP") {
                    setupFindMatjipMode()
                }
            }
        })
    }

    private fun handleMapModeChange() {
        if (!::kakaoMap.isInitialized) return

        val curLatLng = LatLng.from(
            kakaoMap.cameraPosition!!.position.latitude,
            kakaoMap.cameraPosition!!.position.longitude
        )

        kakaoMap.labelManager?.getLodLayer()?.removeAll()

        if (mapMode == "FIND_MATJIP") {
            kakaoMap.setOnCameraMoveEndListener { _, position, _ ->
                val latLng = LatLng.from(position.position.latitude, position.position.longitude)
                kakaoMap.labelManager?.getLodLayer()?.removeAll()
                updateLabelsAtLocation(latLng)
            }
            updateLabelsAtLocation(curLatLng)
        } else {
            kakaoMap.setOnCameraMoveEndListener(null)
        }
    }

    private fun setupFindMatjipMode() {
        kakaoMap.setOnCameraMoveEndListener { map, position, _ ->
            val latLng = LatLng.from(position.position.latitude, position.position.longitude)
            map.labelManager?.getLodLayer()?.removeAll()
            updateLabelsAtLocation(latLng)
        }
    }

    private fun updateLabelsAtLocation(latLng: LatLng) {
        searchPlacesByCategory(latLng) { places ->
            val labelStyles = LabelStyles.from(LabelStyle.from(R.drawable.marker))
            val options = places?.map { place ->
                val p = LatLng.from(place.latitude, place.longitude)
                LabelOptions.from(p).setStyles(labelStyles).setTag(place)
            } ?: emptyList()

            kakaoMap.labelManager?.getLodLayer()?.addLodLabels(options)
        }

        kakaoMap.setOnLodLabelClickListener { _, _, label ->
            (label.tag as? Matjip)?.let {
                showPlaceDetailDialog(it)
            } ?: Log.e("LOD_CLICK", "Tag is not Matjip or is null: ${label.tag}")
            true
        }
    }

    fun showPlaceDetailDialog(place: Matjip) {
        supportFragmentManager.findFragmentByTag("place_detail")?.let {
            if (it is PlaceDetailBottomSheetFragment && it.isAdded) {
                supportFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
            }
        }
        PlaceDetailBottomSheetFragment(place).apply {
            if (!isAdded) show(supportFragmentManager, "place_detail")
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.resume()
        Log.d("map", "map resume")
    }

    override fun onPause() {
        super.onPause()
        mapView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.pause()
    }

    fun searchPlacesByCategory(latlng: LatLng, onResult: (List<Matjip>?) -> Unit) {
        val apiKey = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}"
        val categoryCode = "FD6"
        val (longitude, latitude) = latlng.run { longitude to latitude }

        apiService.searchByCategory(apiKey, categoryCode, longitude, latitude, 1000)
            .enqueue(object : Callback<CategorySearchResponse> {
                override fun onResponse(call: Call<CategorySearchResponse>, response: Response<CategorySearchResponse>) {
                    onResult(response.body()?.documents.takeIf { response.isSuccessful })
                }

                override fun onFailure(call: Call<CategorySearchResponse>, t: Throwable) {
                    Log.e("API_ERROR", t.message ?: "Unknown error")
                    onResult(null)
                }
            })
    }
}
