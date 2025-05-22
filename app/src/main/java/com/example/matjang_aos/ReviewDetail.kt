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
            loadAllReviews(place.placeName)

        } else {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAllReviews(placeName: String) {
        val db = Firebase.firestore
        val sanitizedPlaceName = placeName.replace("/", "_")

        db.collection("review")
            .document(sanitizedPlaceName)
            .collection("users")  // 모든 사용자 리뷰
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "작성된 리뷰가 없습니다.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val stringBuilder = StringBuilder()
                for (doc in documents) {
                    val rate = doc.getDouble("rate") ?: 0.0
                    val comment = doc.getString("comment") ?: "내용 없음"
                    val user = doc.getString("user_email") ?: "익명"

                    stringBuilder.append("👤 $user\n⭐ $rate\n📝 $comment\n\n")
                }

                reviewTextView.text = stringBuilder.toString()
            }
            .addOnFailureListener {
                Toast.makeText(this, "리뷰 불러오기 실패", Toast.LENGTH_SHORT).show()
                Log.e("ReviewDetail", "리뷰 로딩 실패: ${it.message}")
            }
    }

}