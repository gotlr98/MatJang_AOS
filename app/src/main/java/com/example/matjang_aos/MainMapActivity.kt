package com.example.matjang_aos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.matjang_aos.databinding.ActivityMainMapBinding
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.*
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

class MainMapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainMapBinding
    private lateinit var kakaoMap: KakaoMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mapMode: String = "BROWSE"
    private val db = FirebaseFirestore.getInstance()
    private var isGuest: Boolean = false

    private val apiService: KakaoApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://dapi.kakao.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KakaoApiService::class.java)
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) moveToCurrentLocation()
        else Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapMode = intent.getStringExtra("mapType") ?: mapMode
        UserManager.loadUserFromPrefs(this)
        if (UserManager.currentUser == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        isGuest = UserManager.currentUser?.type == Type.Guest

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()
        setupMap()
        setupSearchBar()

        intent.getStringExtra("bookmark_place_name")?.let { name ->
            val x = intent.getDoubleExtra("bookmark_x", 0.0)
            val y = intent.getDoubleExtra("bookmark_y", 0.0)
            val place = Matjip(name, "", x, y, "")
            moveToPlace(place)
        }
    }

    private fun setupSearchBar() {
        binding.searchButton.setOnClickListener {
            val keyword = binding.searchEditText.text.toString().trim()
            if (keyword.isEmpty()) {
                Toast.makeText(this, "검색어를 입력해주세요", Toast.LENGTH_SHORT).show()
            } else {
                searchPlacesByKeyword(keyword)
            }
        }
    }

    private fun setupUI() {
        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
            if (!isGuest) {
                loadBookmarks()
            }
        }

        binding.mapModeSpinner.apply {
            adapter = ArrayAdapter.createFromResource(
                context,
                R.array.map_modes,
                android.R.layout.simple_spinner_item
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    mapMode = if (position == 0) "BROWSE" else "FIND_MATJIP"
                    handleMapModeChange()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        binding.fabMyLocation.setOnClickListener {
            checkLocationPermissionAndMove()
        }

        val email = getSharedPreferences("signIn", Context.MODE_PRIVATE).getString("email", "알 수 없음")
        val header = binding.navigationView.getHeaderView(0)
        header.findViewById<android.widget.TextView>(R.id.email_text).text = "안녕하세요 \n $email 님"
        header.findViewById<android.widget.ImageView>(R.id.profile_icon).setOnClickListener {
            startActivity(Intent(this, MyPageActivity::class.java))
        }
    }

    private fun setupMap() {
        KakaoMapSdk.init(this, BuildConfig.NATIVE_APP_KEY)
        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {}
            override fun onMapError(error: Exception) {}
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                moveToCurrentLocation()
                handleMapModeChange()
                checkLocationPermissionAndMove()
            }
        })
    }

    private fun checkLocationPermissionAndMove() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            moveToCurrentLocation()
        }
    }

    private fun moveToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val latLng = LatLng.from(it.latitude, it.longitude)
                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(latLng))
            } ?: Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "위치 오류: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleMapModeChange() {
        if (!::kakaoMap.isInitialized) return
        kakaoMap.labelManager?.getLodLayer()?.removeAll()

        if (mapMode == "FIND_MATJIP") {
            kakaoMap.setOnCameraMoveEndListener { _, position, _ ->
                val latLng = LatLng.from(position.position.latitude, position.position.longitude)
                kakaoMap.labelManager?.getLodLayer()?.removeAll()
                updateLabelsAtLocation(latLng)
            }
            kakaoMap.cameraPosition?.let {
                updateLabelsAtLocation(LatLng.from(it.position.latitude, it.position.longitude))
            }
        } else {
            kakaoMap.setOnCameraMoveEndListener(null)
        }
    }

    private fun updateLabelsAtLocation(latLng: LatLng) {
        searchPlacesByCategory(latLng) { places ->
            val labelStyles = LabelStyles.from(LabelStyle.from(R.drawable.marker))
            val options = places?.map {
                LabelOptions.from(LatLng.from(it.latitude, it.longitude))
                    .setStyles(labelStyles).setTag(it)
            } ?: emptyList()
            kakaoMap.labelManager?.getLodLayer()?.addLodLabels(options)
        }
        kakaoMap.setOnLodLabelClickListener { _, _, label ->
            (label.tag as? Matjip)?.let { showPlaceDetailDialog(it) }
            true
        }
    }

    private fun showPlaceDetailDialog(place: Matjip) {
        supportFragmentManager.findFragmentByTag("place_detail")?.let {
            if (it is PlaceDetailBottomSheetFragment && it.isAdded) {
                supportFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
            }
        }
        PlaceDetailBottomSheetFragment(place, isGuest).show(supportFragmentManager, "place_detail")
    }

    private fun moveToPlace(matjip: Matjip) {
        val latLng = LatLng.from(matjip.latitude, matjip.longitude)
        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(latLng))
        kakaoMap.labelManager?.getLodLayer()?.addLodLabel(
            LabelOptions.from(latLng)
                .setStyles(LabelStyles.from(LabelStyle.from(R.drawable.marker)))
                .setTag(matjip)
        )
        showPlaceDetailDialog(matjip)
    }

    private fun loadBookmarks() {
        if (isGuest) return

        val email = getSharedPreferences("signIn", Context.MODE_PRIVATE).getString("email", null) ?: return
        val userId = "$email&kakao"
        val menu = binding.navigationView.menu
        menu.clear()

        db.collection("users").document(userId).collection("bookmark").get()
            .addOnSuccessListener { documents ->
                for (group in documents) {
                    val groupName = group.id
                    val groupMenuItem = menu.add(groupName)
                    groupMenuItem.setOnMenuItemClickListener {
                        toggleBookmarkList(groupName, groupMenuItem)
                        true
                    }
                }
            }
    }

    private fun toggleBookmarkList(groupName: String, groupMenuItem: MenuItem) {
        if (isGuest) return

        val email = getSharedPreferences("signIn", Context.MODE_PRIVATE).getString("email", null) ?: return
        val userId = "$email&kakao"
        val menu = binding.navigationView.menu
        val existingItems = mutableListOf<MenuItem>()

        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.title.toString().startsWith("• ") && item.intent?.action == groupName) {
                existingItems.add(item)
            }
        }

        if (existingItems.isNotEmpty()) {
            existingItems.forEach { menu.removeItem(it.itemId) }
            return
        }

        db.collection("users").document(userId).collection("bookmark")
            .document(groupName).get().addOnSuccessListener { document ->
                for (place in document.data?.keys.orEmpty()) {
                    val matjipData = document.get(place)
                    if (matjipData is Map<*, *>) {
                        val matjip = Matjip.fromMap(matjipData).copy(placeName = place)
                        val item = menu.add("• ${matjip.placeName}")
                        item.intent = Intent().apply { action = groupName }
                        item.setOnMenuItemClickListener {
                            binding.drawerLayout.closeDrawer(GravityCompat.START)

                            val listener = object : DrawerLayout.DrawerListener {
                                override fun onDrawerClosed(drawerView: View) {
                                    moveToPlace(matjip)
                                    binding.drawerLayout.removeDrawerListener(this)
                                }
                                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
                                override fun onDrawerOpened(drawerView: View) {}
                                override fun onDrawerStateChanged(newState: Int) {}
                            }

                            binding.drawerLayout.addDrawerListener(listener)
                            true
                        }
                    }
                }
            }
    }

    override fun onResume() { super.onResume(); binding.mapView.resume() }
    override fun onPause() { super.onPause(); binding.mapView.pause() }
    override fun onDestroy() { super.onDestroy(); binding.mapView.pause() }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val matjip = data?.getSerializableExtra("matjip") as? Matjip ?: return
            moveToPlace(matjip)
        }
    }

    private fun searchPlacesByCategory(latlng: LatLng, onResult: (List<Matjip>?) -> Unit) {
        val apiKey = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}"
        val categoryCode = "FD6"
        apiService.searchByCategory(apiKey, categoryCode, latlng.longitude, latlng.latitude, 5000)
            .enqueue(object : Callback<CategorySearchResponse> {
                override fun onResponse(call: Call<CategorySearchResponse>, response: Response<CategorySearchResponse>) {
                    onResult(response.body()?.documents)
                }

                override fun onFailure(call: Call<CategorySearchResponse>, t: Throwable) {
                    Log.e("API_ERROR", t.message ?: "Unknown error")
                    onResult(null)
                }
            })
    }

    private fun searchPlacesByKeyword(keyword: String) {
        val apiKey = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}"
        val center = kakaoMap.cameraPosition?.position ?: return
        apiService.searchByKeyword(apiKey, keyword, center.longitude, center.latitude)
            .enqueue(object : Callback<CategorySearchResponse> {
                override fun onResponse(call: Call<CategorySearchResponse>, response: Response<CategorySearchResponse>) {
                    val results = response.body()?.documents
                    if (!results.isNullOrEmpty()) moveToPlace(results.first())
                }

                override fun onFailure(call: Call<CategorySearchResponse>, t: Throwable) {
                    Toast.makeText(this@MainMapActivity, "검색 실패: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}

interface KakaoApiService {
    @GET("v2/local/search/category.json")
    fun searchByCategory(
        @Header("Authorization") apiKey: String,
        @Query("category_group_code") category: String,
        @Query("x") longitude: Double,
        @Query("y") latitude: Double,
        @Query("radius") radius: Int
    ): Call<CategorySearchResponse>

    @GET("v2/local/search/keyword.json")
    fun searchByKeyword(
        @Header("Authorization") apiKey: String,
        @Query("query") keyword: String,
        @Query("x") longitude: Double,
        @Query("y") latitude: Double,
        @Query("radius") radius: Int = 5000
    ): Call<CategorySearchResponse>
}
