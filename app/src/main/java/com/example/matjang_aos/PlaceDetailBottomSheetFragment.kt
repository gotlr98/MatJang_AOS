package com.example.matjang_aos

import android.content.Intent
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.matjang_aos.databinding.FragmentPlaceDetailBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PlaceDetailBottomSheetFragment(private val place: Matjip) : BottomSheetDialogFragment() {

    private var _binding: FragmentPlaceDetailBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        UserManager.loadUserFromPrefs(requireContext())
        Log.d("BottomSheet", "email: ${UserManager.currentUser?.email}")
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
        val view = inflater.inflate(R.layout.fragment_place_detail_bottom_sheet, container, false)



        // View에서 필요한 부분 찾고, 데이터를 설정
        val placeNameTextView: TextView = view.findViewById(R.id.place_name)
        val addressTextView: TextView = view.findViewById(R.id.address)

        _binding = FragmentPlaceDetailBottomSheetBinding.inflate(inflater, container, false)
        setupUI() // ✅ 이 위치 중요!

        return binding.root
    }

    private fun setupUI() {
        binding.placeName.text = place.placeName
        binding.address.text = place.address

        // ✅ 장소 클릭 시 리뷰 확인 및 이동
        binding.root.setOnClickListener {
            val userEmail = UserManager.currentUser?.email
            if (userEmail.isNullOrBlank()) {
                Toast.makeText(requireContext(), "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                dismiss()
                return@setOnClickListener
            }

            val placeName = place.placeName
            ReviewUtil.checkIfUserReviewed(userEmail, placeName) { hasReviewed ->
                val intent = if (hasReviewed) {
                    Intent(requireContext(), ReviewDetail::class.java).apply {
                        putExtra("place", place)
                    }
                } else {
                    Intent(requireContext(), ReviewWrite::class.java).apply {
                        putExtra("place", place)
                    }
                }
                startActivity(intent)
                dismiss()
            }
        }

        // ✅ 북마크 버튼 클릭 시 유저 정보 없으면 처리
        binding.bookmarkButton.setOnClickListener {
            val user = UserManager.currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "로그인 후 이용해주세요.", Toast.LENGTH_SHORT).show()
                dismiss()
                return@setOnClickListener
            }

            showBookmarkDialog()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }

    private fun showBookmarkDialog() {
        val context = requireContext()
        val inputEditText = EditText(context)
        inputEditText.hint = "그룹 이름을 입력하세요"

        AlertDialog.Builder(context)
            .setTitle("북마크 추가")
            .setView(inputEditText)
            .setPositiveButton("추가") { _, _ ->
                val groupName = inputEditText.text.toString().trim()
                if (groupName.isNotEmpty()) {
                    savePlaceToBookmarkGroup(groupName)
                } else {
                    Toast.makeText(context, "그룹 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun savePlaceToBookmarkGroup(groupName: String) {
        val user = UserManager.currentUser ?: return
        val db = Firebase.firestore

        // 가게 정보
        val placeData = mapOf(
            "address" to place.address,
            "category_name" to place.category,
            "x" to place.longitude,
            "y" to place.latitude
        )

        db.collection("users")
            .document("${user.email}&Kakao")
            .collection("bookmark")
            .document(groupName)
            .update(place.placeName, placeData) // place_name을 필드로 추가
            .addOnSuccessListener {
                Toast.makeText(context, "북마크에 추가되었습니다", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("Bookmark", "북마크 추가 실패: ${e.message}")
                Toast.makeText(context, "실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }





}