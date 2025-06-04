package com.example.matjang_aos

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

class MyPage : AppCompatActivity() {
    private lateinit var reviewContainer: LinearLayout
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        reviewContainer = findViewById(R.id.review_container)

        val prefs = getSharedPreferences("signIn", Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val type = prefs.getString("type", null)

        if (email == null || type == null) {
            Toast.makeText(this, "로그인 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val docPath = "$email&$type"

        firestore.collection("users")
            .document(docPath)
            .collection("review")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    val textView = TextView(this).apply {
                        text = "작성한 리뷰가 없습니다."
                        textSize = 16f
                    }
                    reviewContainer.addView(textView)
                } else {
                    val inflater = LayoutInflater.from(this)

                    for (doc in documents) {
                        val placeName = doc.id
                        val rate = doc.getDouble("rate") ?: 0.0
                        val comment = doc.getString("comment") ?: ""

                        val cardView = inflater.inflate(R.layout.review_card, reviewContainer, false)

                        cardView.findViewById<TextView>(R.id.place_name).text = "📍 $placeName"
                        cardView.findViewById<TextView>(R.id.rate_text).text = "⭐ 평점: $rate"
                        cardView.findViewById<TextView>(R.id.comment_text).text = "💬 $comment"

                        reviewContainer.addView(cardView)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "리뷰 불러오기 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}