package com.example.matjang_aos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlaceDetailBottomSheetFragment(private val place: Matjip) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_place_detail_bottom_sheet, container, false)

        // View에서 필요한 부분 찾고, 데이터를 설정
        val placeNameTextView: TextView = view.findViewById(R.id.place_name)
        val addressTextView: TextView = view.findViewById(R.id.address)
        val phoneTextView: TextView = view.findViewById(R.id.phone)

        placeNameTextView.text = place.placeName
        addressTextView.text = place.address
        return view
    }


}