package com.example.matjang_aos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.matjang_aos.databinding.ActivityReviewWriteBinding // ✅ ViewBinding import
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ReviewWriteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewWriteBinding
    private lateinit var place: Matjip

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewWriteBinding.inflate(layoutInflater) // binding inflate
        setContentView(binding.root) // binding 적용

        // WindowInsets 적용
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets -> // binding.main 사용
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 툴바 설정
        setSupportActionBar(binding.reviewToolbar) // binding 적용
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "리뷰 작성"

        // 장소 정보 받기
        place = intent.getSerializableExtra("place") as Matjip

        // RatingBar 설정
        binding.ratingBar.stepSize = 0.5f
        binding.ratingBar.numStars = 5

        // 제출 버튼 클릭
        binding.submitBtn.setOnClickListener {
            submitReview()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun submitReview() {
        val rating = binding.ratingBar.rating.toDouble() // binding
        val comment = binding.reviewEditText.text.toString() // binding
        val user = UserManager.currentUser

        if (user == null || user.email.isNullOrBlank()) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val emailWithType = "${user.email}&${user.type}"
        val review = Review(
            placeName = place.placeName,
            rate = rating,
            comment = comment,
            user_email = emailWithType,
            address = place.address ?: ""
        )

        val db = Firebase.firestore
        val userRef = db.collection("users").document(emailWithType)
        val placeReviewRef = db.collection("reviews")
            .document(place.placeName.replace("/", "_"))
            .collection("userReviews")
            .document(emailWithType)

        placeReviewRef.set(review)
            .addOnSuccessListener {
                userRef.update("reviews", com.google.firebase.firestore.FieldValue.arrayUnion(review))
                    .addOnSuccessListener {
                        val updatedUser = user.copy(reviews = user.reviews + review)
                        UserManager.login(updatedUser)
                        UserManager.saveUserToPrefs(this)

                        Toast.makeText(this, "리뷰가 등록되었습니다.", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, MainMapActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        intent.putExtra("mapType", "FIND_MATJIP")
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "유저 리뷰 배열 업데이트 실패", Toast.LENGTH_SHORT).show()
                        Log.e("Firestore", "유저 배열 저장 실패", e)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "리뷰 저장 실패", Toast.LENGTH_SHORT).show()
                Log.e("Firestore", "리뷰 저장 실패", e)
            }
    }
}
