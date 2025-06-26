package com.example.matjang_aos

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setDimAmount(0f)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.setBackgroundColor(android.graphics.Color.WHITE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceDetailBottomSheetBinding.inflate(inflater, container, false)
        UserManager.loadUserFromPrefs(requireContext())
        checkBookmarkStatus()
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        binding.placeName.text = place.placeName
        binding.address.text = place.address
        updateBookmarkIcon()

        binding.root.setOnClickListener {
            if (!isUserLoggedIn()) return@setOnClickListener

            val user = UserManager.currentUser!!
            ReviewUtil.checkIfUserReviewed(user.email, user.type.name, place.placeName) { hasReviewed ->
                val intent = Intent(requireContext(),
                    if (hasReviewed) ReviewDetailActivity::class.java else ReviewWriteActivity::class.java
                ).apply {
                    putExtra("place", place)
                }
                startActivity(intent)
                dismiss()
            }
        }

        binding.bookmarkButton.setOnClickListener {
            if (!isUserLoggedIn()) return@setOnClickListener

            if (isBookmarked) {
                Toast.makeText(requireContext(), "이미 북마크에 등록된 장소입니다.", Toast.LENGTH_SHORT).show()
            } else {
                checkBookmarkGroupAndShowDialog()
            }
        }
    }

    private fun checkBookmarkStatus() {
        val user = UserManager.currentUser ?: return
        val db = Firebase.firestore

        db.collection("users")
            .document("${user.email}&kakao")
            .collection("bookmark")
            .get()
            .addOnSuccessListener { snapshot ->
                isBookmarked = snapshot.any { it.contains(place.placeName) }
                updateBookmarkIcon()
            }
    }

    private fun updateBookmarkIcon() {
        binding.bookmarkButton.setImageResource(
            if (isBookmarked) R.drawable.bookmark else R.drawable.bookmark_border
        )
    }

    private fun checkBookmarkGroupAndShowDialog() {
        val user = UserManager.currentUser ?: return
        val db = Firebase.firestore

        db.collection("users")
            .document("${user.email}&kakao")
            .collection("bookmark")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) showBookmarkDialog()
                else showBookmarkGroupSelectionDialog()
            }
    }

    private fun showBookmarkDialog() {
        val context = requireContext()
        val inputEditText = EditText(context).apply { hint = "그룹 이름을 입력하세요" }

        AlertDialog.Builder(context)
            .setTitle("북마크 그룹 추가")
            .setView(inputEditText)
            .setPositiveButton("추가") { _, _ ->
                val groupName = inputEditText.text.toString().trim()
                if (groupName.isNotEmpty()) {
                    savePlaceToBookmarkGroup(groupName) {
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
                AlertDialog.Builder(requireContext())
                    .setTitle("북마크 그룹 선택")
                    .setItems(groupNames.toTypedArray()) { _, which ->
                        val selectedGroup = groupNames[which]
                        savePlaceToBookmarkGroup(selectedGroup)
                    }
                    .setPositiveButton("그룹 추가") { _, _ -> showBookmarkDialog() }
                    .setNegativeButton("취소", null)
                    .show()
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
                onComplete?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e("Bookmark", "북마크 추가 실패: ${e.message}")
                Toast.makeText(context, "실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isUserLoggedIn(): Boolean {
        val user = UserManager.currentUser
        return if (user?.email.isNullOrBlank()) {
            Toast.makeText(requireContext(), "로그인 후 이용해주세요.", Toast.LENGTH_SHORT).show()
            dismiss()
            false
        } else true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
