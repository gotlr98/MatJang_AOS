package com.example.matjang_aos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.matjang_aos.databinding.FragmentPlaceDetailBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlaceDetailBottomSheetFragment(private val place: Matjip) : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentPlaceDetailBottomSheetBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
        val view = inflater.inflate(R.layout.fragment_place_detail_bottom_sheet, container, false)

        // View에서 필요한 부분 찾고, 데이터를 설정
        val placeNameTextView: TextView = view.findViewById(R.id.place_name)
        val addressTextView: TextView = view.findViewById(R.id.address)

        setupUI()

        return binding.root
    }

    private fun setupUI() {
        binding.placeName.text = place.placeName
        binding.address.text = place.address

        // BottomSheet 전체를 클릭했을 때 처리
        binding.root.setOnClickListener {
            checkReviewAndNavigate()
        }

        // 즐겨찾기 버튼
        binding.favoriteButton.setOnClickListener {
            addToFavorites(place)
        }
    }


}