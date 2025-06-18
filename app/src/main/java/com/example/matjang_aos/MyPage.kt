package com.example.matjang_aos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MyPage : AppCompatActivity() {
    private lateinit var reviewContainer: LinearLayout
    private lateinit var followingContainer: LinearLayout
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        reviewContainer = findViewById(R.id.review_container)
        followingContainer = findViewById(R.id.following_container)

        val email = UserManager.currentUser?.email
        val type = UserManager.currentUser?.type

        if (email == null || type == null) {
            Toast.makeText(this, "로그인 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val docPath = "$email&$type"

        val followButton: ImageButton = findViewById(R.id.btn_add_follow)
        followButton.setOnClickListener {
            val intent = Intent(this, FindFollowerView::class.java)
            startActivity(intent)
        }

        // ✅ 내가 작성한 리뷰 불러오기
        firestore.collection("users")
            .document(docPath)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(UserModel::class.java)

                // 리뷰
                val reviews = user?.reviews
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

                // ✅ 팔로잉 목록
                val followingList = user?.following ?: emptyList()
                if (followingList.isEmpty()) {
                    val textView = TextView(this).apply {
                        text = "팔로잉한 유저가 없습니다."
                        textSize = 16f
                    }
                    followingContainer.addView(textView)
                } else {
                    val inflater = LayoutInflater.from(this)
                    for (followEmail in followingList) {
                        // 이메일로 유저 정보 조회 (type도 알아야 함)
                        firestore.collection("users")
                            .whereEqualTo("email", followEmail)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                val doc = querySnapshot.documents.firstOrNull() ?: return@addOnSuccessListener
                                val followUser = doc.toObject(UserModel::class.java) ?: return@addOnSuccessListener
                                val followType = followUser.type ?: "kakao"
                                val followDocPath = "$followEmail&$followType"

                                val cardView = inflater.inflate(R.layout.follow_card, followingContainer, false)
                                cardView.findViewById<TextView>(R.id.user_email).text = followEmail
                                cardView.findViewById<TextView>(R.id.review_count).text = "리뷰 ${followUser.reviews?.size ?: 0}개"

                                val unfollowBtn = cardView.findViewById<ImageButton>(R.id.follow_button)
                                unfollowBtn.setImageResource(R.drawable.cancel) // 언팔로우 아이콘
                                unfollowBtn.setOnClickListener {
                                    unfollowUser(docPath, followDocPath, followEmail)
                                }

                                followingContainer.addView(cardView)
                            }
                    }
                }
            }
    }

    private fun unfollowUser(myDocPath: String, targetDocPath: String, targetEmail: String) {
        val currentUserRef = firestore.collection("users").document(myDocPath)
        val targetUserRef = firestore.collection("users").document(targetDocPath)

        firestore.runBatch { batch ->
            batch.update(currentUserRef, "following", FieldValue.arrayRemove(targetEmail))
            batch.update(targetUserRef, "follower", FieldValue.arrayRemove(UserManager.currentUser?.email))
        }.addOnSuccessListener {
            Toast.makeText(this, "$targetEmail 언팔로우 완료!", Toast.LENGTH_SHORT).show()
            recreate()
        }.addOnFailureListener {
            Toast.makeText(this, "언팔로우 실패: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
