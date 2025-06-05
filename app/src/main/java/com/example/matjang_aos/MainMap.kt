package com.example.matjang_aos

import android.content.Context
import android.content.Intent
import android.icu.text.Transliterator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
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
    private lateinit var bookmarkContainer: LinearLayout
    private lateinit var navigationView: NavigationView
    private lateinit var kakaoMap: KakaoMap
    private var mapMode: String = "BROWSE"
    private val db = FirebaseFirestore.getInstance()

    private lateinit var drawerLayout: DrawerLayout


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

        intent.getStringExtra("bookmark_place_name")?.let { name ->
            val x = intent.getDoubleExtra("bookmark_x", 0.0)
            val y = intent.getDoubleExtra("bookmark_y", 0.0)
            val place = Matjip(name, "", x, y, "")
            val position = LatLng.from(y, x)
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(position))

            val labelStyles = LabelStyles.from(LabelStyle.from(R.drawable.marker))
            val labelOption = LabelOptions.from(position).setStyles(labelStyles).setTag(place)
            kakaoMap.labelManager?.getLodLayer()?.removeAll()
            kakaoMap.labelManager?.getLodLayer()?.addLodLabel(labelOption)

            showPlaceDetailDialog(place)
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
        navigationView.getHeaderView(0).findViewById<TextView>(R.id.email_text).text = "안녕하세요 \n $email 님"

        val profileIcon = navigationView.getHeaderView(0).findViewById<ImageView>(R.id.profile_icon)
        profileIcon.setOnClickListener {
            val intent = Intent(this, MyPage::class.java)
            startActivity(intent)
        }



        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_settings -> Toast.makeText(this, "설정 클릭됨", Toast.LENGTH_SHORT).show()
                R.id.nav_logout -> Toast.makeText(this, "로그아웃 클릭됨", Toast.LENGTH_SHORT).show()
                R.id.nav_bookmarks -> {
                    val intent = Intent(this, BookmarkList::class.java)
                    startActivity(intent)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        bookmarkContainer = navigationView.getHeaderView(0).findViewById<LinearLayout>(R.id.bookmark_container)
        val bookmarkBtn = navigationView.getHeaderView(0).findViewById<Button>(R.id.bookmark_list_button)
        bookmarkBtn.setOnClickListener {
            loadBookmarks()
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

    private fun loadBookmarks() {
        val email = getSharedPreferences("signIn", Context.MODE_PRIVATE).getString("email", null) ?: return
        val userId = email + "&kakao"

        db.collection("users").document(userId).collection("bookmark").get()
            .addOnSuccessListener { documents ->
                bookmarkContainer.removeAllViews()
                for (group in documents) {
                    val groupName = group.id
                    val groupButton = Button(this).apply {
                        text = groupName
                        setOnClickListener {
                            toggleBookmarkList(groupName)
                        }
                    }
                    bookmarkContainer.addView(groupButton)

                    val listLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        visibility = View.GONE
                        tag = "list_$groupName"
                    }
                    bookmarkContainer.addView(listLayout)
                }
            }
    }

    private fun toggleBookmarkList(groupName: String) {
        val email = getSharedPreferences("signIn", Context.MODE_PRIVATE).getString("email", null) ?: return
        val userId = email + "&kakao"
        val listLayout = bookmarkContainer.findViewWithTag<LinearLayout>("list_$groupName")

        if (listLayout.visibility == View.VISIBLE) {
            listLayout.visibility = View.GONE
            return
        }

        listLayout.removeAllViews()
        listLayout.visibility = View.VISIBLE

        db.collection("users").document(userId).collection("bookmark")
            .document(groupName).get().addOnSuccessListener { document ->
                for (place in document.data?.keys.orEmpty()) {
                    val matjipData = document.get(place)
                    if (matjipData is Map<*, *>) {
                        val matjip = Matjip.fromMap(matjipData)
                        val placeButton = Button(this).apply {
                            text = matjip.placeName
                            setOnClickListener { moveToPlace(matjip) }
                        }
                        listLayout.addView(placeButton)
                    }
                }
            }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val matjip = data?.getSerializableExtra("matjip") as? Matjip ?: return
            moveToPlace(matjip)
        }
    }

    private fun moveToPlace(matjip: Matjip) {
        val latLng = LatLng.from(matjip.latitude, matjip.longitude)
        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(latLng))

        kakaoMap.labelManager?.getLodLayer()?.addLodLabel(
            LabelOptions.from(latLng).setStyles(LabelStyles.from(LabelStyle.from(R.drawable.marker))).setTag(matjip)
        )
        showPlaceDetailDialog(matjip)
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
