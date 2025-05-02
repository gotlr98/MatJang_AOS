package com.example.matjang_aos

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
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
        enableEdgeToEdge()
        setContentView(R.layout.activity_review_write)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        place = intent.getSerializableExtra("place") as Matjip

        ratingBar = findViewById(R.id.ratingBar)
        reviewEditText = findViewById(R.id.reviewEditText)
        submitBtn = findViewById(R.id.submitBtn)

        ratingBar.stepSize = 0.5f
        ratingBar.numStars = 5

        submitBtn.setOnClickListener {
            showConfirmDialog()
        }
    }

    private fun showConfirmDialog(){
        val rating = ratingBar.rating.toDouble()
        val comment = reviewEditText.text.toString()
        val user = UserManager.currentUser ?: return

        val email = user.email
        if (email.isNullOrBlank()) {
            Log.d("email", "email is null")
            return
        }

        val review = mapOf(
            "rate" to rating,
            "comment" to comment,
            "user_email" to user.email
        )

        val db = Firebase.firestore

        db.collection("review")
            .document(place.placeName.replace("/","_"))
            .collection("reviews")
            .document(user.email ?: "")
            .set(review)

        db.collection("users")
            .document("${user.email}&kakao")
            .collection("review")
            .document(place.placeName)
            .set(review)
            .addOnSuccessListener {
                val newReview = Review(
                    placeName = place.placeName,
                    rate = rating,
                    comment = comment,
                    user_email = user.email ?: "",
                    address = place.address ?: ""
                )

                val updatedUser = user.copy(
                    reviews = user.reviews + newReview // 전체 리뷰 정보를 추가
                )

                UserManager.login(updatedUser)
                UserManager.saveUserToPrefs(this)
            }
    }
}