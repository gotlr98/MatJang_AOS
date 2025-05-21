package com.example.matjang_aos

import android.content.Intent
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
    private lateinit var cancelBtn: Button


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

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.reviewToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기 아이콘 표시
        supportActionBar?.title = "리뷰 작성"

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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showConfirmDialog() {
        val rating = ratingBar.rating.toDouble()
        val comment = reviewEditText.text.toString()
        val user = UserManager.currentUser

        if (user == null) {
            Log.e("ReviewWrite", "User is null")
            return
        }

        val email = user.email
        if (email.isNullOrBlank()) {
            Log.d("email", "email is null")
            return
        }

        val sanitizedPlaceName = place.placeName.replace("/", "_") // ✅ 통일된 이름

        val review = mapOf(
            "rate" to rating,
            "comment" to comment,
            "user_email" to email
        )

        val db = Firebase.firestore

        db.collection("review")
            .document(sanitizedPlaceName)
            .collection("reviews")
            .document("${email}&kakao")
            .set(review)

        db.collection("users")
            .document("${email}&kakao") // ✅ 일관된 casing
            .collection("review")
            .document(sanitizedPlaceName) // ✅ 동일한 이름 사용
            .set(review)
            .addOnSuccessListener {
                val newReview = Review(
                    placeName = place.placeName,
                    rate = rating,
                    comment = comment,
                    user_email = email,
                    address = place.address ?: ""
                )

                val updatedUser = user.copy(
                    reviews = user.reviews + newReview
                )

                UserManager.login(updatedUser)
                UserManager.saveUserToPrefs(this)

                // ✅ MainMap 화면으로 이동
                val intent = Intent(this, MainMap::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "리뷰 저장 실패", Toast.LENGTH_SHORT).show()
                Log.e("Firestore", "리뷰 저장 실패", it)
            }
    }


}