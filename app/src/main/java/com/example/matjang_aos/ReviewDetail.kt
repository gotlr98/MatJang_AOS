package com.example.matjang_aos

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class ReviewDetail : AppCompatActivity() {
    private lateinit var placeNameTextView: TextView
    private lateinit var addressTextView: TextView
    private lateinit var rateTextView: TextView
    private lateinit var reviewTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_detail)

        // View 연결
        placeNameTextView = findViewById(R.id.place_name)
        addressTextView = findViewById(R.id.address)
        rateTextView = findViewById(R.id.rate)
        reviewTextView = findViewById(R.id.review)

        // Intent로부터 Matjip 객체 가져오기
        val place = intent.getSerializableExtra("place") as? Matjip

        if (place == null) {
            Toast.makeText(this, "장소 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        placeNameTextView.text = place.placeName
        addressTextView.text = place.address

        val userEmail = UserManager.currentUser?.email
        if (userEmail != null) {
            loadReview(userEmail, place.placeName)
        } else {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadReview(userEmail: String, placeName: String) {
        val db = Firebase.firestore
        val sanitizedPlaceName = placeName.replace("/", "_")

        db.collection("users")
            .document("${userEmail}&Kakao")
            .collection("review")
            .document(sanitizedPlaceName)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val rate = document.getDouble("rate") ?: 0.0
                    val review = document.getString("comment") ?: "내용 없음"

                    rateTextView.text = "평점: $rate"
                    reviewTextView.text = review
                } else {
                    Toast.makeText(this, "리뷰 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "리뷰 불러오기 실패", Toast.LENGTH_SHORT).show()
                Log.e("ReviewDetail", "리뷰 로딩 실패: ${it.message}")
            }
    }
}