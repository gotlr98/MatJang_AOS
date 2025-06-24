package com.example.matjang_aos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ReviewWrite : AppCompatActivity() {

    private lateinit var ratingBar: RatingBar
    private lateinit var reviewEditText: EditText
    private lateinit var submitBtn: Button

    private lateinit var place: Matjip

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_write)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.reviewToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "리뷰 작성"

        place = intent.getSerializableExtra("place") as Matjip

        ratingBar = findViewById(R.id.ratingBar)
        reviewEditText = findViewById(R.id.reviewEditText)
        submitBtn = findViewById(R.id.submitBtn)

        ratingBar.stepSize = 0.5f
        ratingBar.numStars = 5

        submitBtn.setOnClickListener {
            submitReview()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun submitReview() {
        val rating = ratingBar.rating.toDouble()
        val comment = reviewEditText.text.toString()
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
            .document(place.placeName.replace("/", "_")) // 슬래시 방지
            .collection("userReviews")
            .document(emailWithType)

        // 1. 리뷰 저장: place → review/placeName/userReviews/{emailWithType}
        placeReviewRef.set(review)
            .addOnSuccessListener {
                // 2. 유저 정보에 리뷰 배열 추가
                userRef.update("reviews", com.google.firebase.firestore.FieldValue.arrayUnion(review))
                    .addOnSuccessListener {
                        val updatedUser = user.copy(reviews = user.reviews + review)
                        UserManager.login(updatedUser)
                        UserManager.saveUserToPrefs(this)

                        Toast.makeText(this, "리뷰가 등록되었습니다.", Toast.LENGTH_SHORT).show()

                        // MainMap 이동
                        val intent = Intent(this, ActivityMainMapBinding::class.java)
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
