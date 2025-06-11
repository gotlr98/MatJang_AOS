package com.example.matjang_aos

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.kakao.sdk.user.model.User

class MyPage : AppCompatActivity() {
    private lateinit var reviewContainer: LinearLayout
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        reviewContainer = findViewById(R.id.review_container)

        val email = UserManager.currentUser?.email
        val type = UserManager.currentUser?.type

        if (email == null || type == null) {
            Toast.makeText(this, "로그인 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val docPath = "$email&$type"



        firestore.collection("users")
            .document(docPath)
            .get()
            .addOnSuccessListener { document ->
                val reviews = document.toObject(UserModel::class.java)?.reviews
                if (reviews.isNullOrEmpty()) {
                    val textView = TextView(this).apply {
                        text = "작성한 리뷰가 없습니다."
                        textSize = 16f
                    }
                    reviewContainer.addView(textView)
                } else {
                    val inflater = LayoutInflater.from(this)
                    for (review in reviews) {
                        val cardView = inflater.inflate(R.layout.review_card, reviewContainer, false)
                        cardView.findViewById<TextView>(R.id.place_name).text = "📍 ${review.placeName}"
                        cardView.findViewById<TextView>(R.id.rate_text).text = "⭐ 평점: ${review.rate}"
                        cardView.findViewById<TextView>(R.id.comment_text).text = "💬 ${review.comment}"

                        reviewContainer.addView(cardView)
                    }
                }
            }

    }
}