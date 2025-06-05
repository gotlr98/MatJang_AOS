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
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PlaceDetailBottomSheetFragment(private val place: Matjip) : BottomSheetDialogFragment() {

    private var _binding: FragmentPlaceDetailBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var isBookmarked = false

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

        checkBookmarkStatus()
        setupUI()

        return binding.root
    }

    private fun checkBookmarkStatus() {
        val user = UserManager.currentUser ?: return
        val db = Firebase.firestore

        val placeName = place.placeName
        if (placeName.isNullOrBlank()) {
            Log.e("BookmarkCheck", "placeName is null or blank")
            return
        }

        db.collection("users")
            .document("${user.email}&kakao")
            .collection("bookmark")
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    if (doc.contains(placeName)) {
                        isBookmarked = true
                        break
                    }
                }
                updateBookmarkIcon()
            }
    }



    private fun setupUI() {
        binding.placeName.text = place.placeName
        binding.address.text = place.address

        updateBookmarkIcon()

        // ✅ 장소 클릭 시 리뷰 확인 및 이동
        binding.root.setOnClickListener {
            val userEmail = UserManager.currentUser?.email
            if (userEmail.isNullOrBlank()) {
                Toast.makeText(requireContext(), "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                dismiss()
                return@setOnClickListener
            }

            val user = UserManager.currentUser
            if (user == null || user.email.isNullOrBlank()) {
                Toast.makeText(requireContext(), "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                dismiss()
                return@setOnClickListener
            }

            ReviewUtil.checkIfUserReviewed(user.email, user.type.name, place.placeName) { hasReviewed ->
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

            if (isBookmarked) {
                Toast.makeText(requireContext(), "이미 북마크에 등록된 장소입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 아직 등록 안 된 경우
            checkBookmarkGroupAndShowDialog()
        }
    }

    private fun checkBookmarkGroupAndShowDialog() {
        val user = UserManager.currentUser ?: return
        val db = Firebase.firestore

        db.collection("users")
            .document("${user.email}&kakao")
            .collection("bookmark")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    // 그룹이 하나도 없는 경우: 새 그룹 생성 다이얼로그
                    showBookmarkDialog()
                } else {
                    // 그룹이 있으니 선택 다이얼로그 보여줌
                    showBookmarkGroupSelectionDialog()
                }
            }
    }


    private fun updateBookmarkIcon() {
        val iconRes = if (isBookmarked) {
            R.drawable.bookmark // 북마크 된 상태 아이콘
        } else {
            R.drawable.bookmark_border // 북마크 안된 상태 아이콘
        }
        binding.bookmarkButton.setImageResource(iconRes)
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
            .setTitle("북마크 그룹 추가")
            .setView(inputEditText)
            .setPositiveButton("추가") { _, _ ->
                val groupName = inputEditText.text.toString().trim()
                if (groupName.isNotEmpty()) {
                    savePlaceToBookmarkGroup(groupName) {
                        // 그룹 추가 후 다시 그룹 선택 다이얼로그로 전환
                        showBookmarkGroupSelectionDialog()
                    }
                } else {
                    Toast.makeText(context, "그룹 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showBookmarkGroupSelectionDialog() {
        val user = UserManager.currentUser ?: return
        val db = Firebase.firestore

        db.collection("users")
            .document("${user.email}&kakao")
            .collection("bookmark")
            .get()
            .addOnSuccessListener { result ->
                val groupNames = result.documents.map { it.id }

                val dialogBuilder = AlertDialog.Builder(requireContext())
                dialogBuilder.setTitle("북마크 그룹 선택")

                val groupArray = groupNames.toTypedArray()
                dialogBuilder.setItems(groupArray) { _, which ->
                    val selectedGroup = groupArray[which]
                    savePlaceToBookmarkGroup(selectedGroup) // 선택한 그룹에 다시 저장
                }

                dialogBuilder.setPositiveButton("그룹 추가") { _, _ ->
                    showBookmarkDialog() // 다시 그룹 추가 다이얼로그
                }

                dialogBuilder.setNegativeButton("취소", null)

                dialogBuilder.show()
            }
    }

    private fun savePlaceToBookmarkGroup(groupName: String, onComplete: (() -> Unit)? = null) {
        val user = UserManager.currentUser ?: return
        val db = Firebase.firestore

        val placeData = mapOf(
            "address" to place.address,
            "category_name" to place.category,
            "x" to place.longitude,
            "y" to place.latitude
        )

        db.collection("users")
            .document("${user.email}&kakao")
            .collection("bookmark")
            .document(groupName)
            .set(mapOf(place.placeName to placeData), SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(context, "북마크에 추가되었습니다", Toast.LENGTH_SHORT).show()
                isBookmarked = true
                updateBookmarkIcon()
                onComplete?.invoke() // ✅ 그룹 추가 후 콜백 실행
            }
            .addOnFailureListener { e ->
                Log.e("Bookmark", "북마크 추가 실패: ${e.message}")
                Toast.makeText(context, "실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}