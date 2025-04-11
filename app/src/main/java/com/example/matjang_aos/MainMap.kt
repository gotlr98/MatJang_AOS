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

    private var map : com.kakao.vectormap.MapView? = null
    private var kakaoMapValue : KakaoMap? = null
//    private var _binding: ResultProfileBinding? = null
//    // This property is only valid between onCreateView and
//// onDestroyView.
//    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        KakaoMapSdk.init(this, BuildConfig.NATIVE_APP_KEY)
        Log.d("TestActivity", "지도 세팅")
        map = findViewById(R.id.map_view) // 맵뷰
        //binding.mapMvMapcontainer.addView(map)
        map?.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() { //맵뷰가 정상적으로 종료 시 사용됨
                Log.e("TestActivity", "onMapDestroy")
            }

            override fun onMapError(error: Exception?) {
                Log.e("TestActivity", "onMApError", error)

            }

        }, object : KakaoMapReadyCallback() {
            //position과 zoom부분은 필수 작성 x

            // 인증 후 API 가 정상적으로 실행될 때 호출됨
            override fun getPosition(): LatLng {
                //startPosition = super.getPosition()
                return super.getPosition()
                //지도가 시작할 때 처음의 시작 위치를 설정한다.
            }

            override fun getZoomLevel(): Int {
                //줌 레벨 설정
                return 17
            }

            override fun onMapReady(kakaoMap: KakaoMap) {
                Log.e("TestActivity", "onMapReady")
                kakaoMapValue = kakaoMap
                // 지도 클릭 리스너 설정
                kakaoMapValue!!.setOnMapClickListener { kakaoMap, latLng, pointF, poi -> //pointF => 현재 핸드폰 화면 상의 좌표값, poi는 지도상의 좌표값, 라벨 이름 등의 정보
                    // POI 정보창 표시
//                    showInfoWindow(poi, pointF)
//                    location = poi.name
//                    latLngString = "${latLng.latitude}"+" "+"${latLng.longitude}"
                }

            }
        })

    }





}