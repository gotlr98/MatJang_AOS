package com.example.matjang_aos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.matjang_aos.databinding.ActivityMyPageBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MyPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyPageBinding
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupButtons()
        loadUserData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar.customToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "마이페이지"
    }

    private fun setupButtons() {
        binding.btnAddFollow.setOnClickListener {
            startActivity(Intent(this, FindFollowerView::class.java))
        }

        binding.btnDeleteAccount.setOnClickListener {
            deleteAccount()
        }
    }

    private fun loadUserData() {
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
                val user = document.toObject(UserModel::class.java)
                displayUserReviews(user?.reviews)
                displayFollowingUsers(user?.following ?: emptyList())
            }
    }

    private fun displayUserReviews(reviews: List<Review>?) {
        binding.reviewContainer.removeAllViews()

        if (reviews.isNullOrEmpty()) {
            binding.reviewContainer.addView(TextView(this).apply {
                text = "작성한 리뷰가 없습니다."
                textSize = 16f
            })
            return
        }

        val inflater = LayoutInflater.from(this)
        for (review in reviews) {
            val cardView = inflater.inflate(R.layout.review_card, binding.reviewContainer, false)
            cardView.findViewById<TextView>(R.id.place_name).text = "📍 ${review.placeName}"
            cardView.findViewById<TextView>(R.id.rate_text).text = "⭐ 평점: ${review.rate}"
            cardView.findViewById<TextView>(R.id.comment_text).text = "💬 ${review.comment}"
            binding.reviewContainer.addView(cardView)
        }
    }

    private fun displayFollowingUsers(followingList: List<String>) {
        binding.followingContainer.removeAllViews()

        if (followingList.isEmpty()) {
            binding.followingContainer.addView(TextView(this).apply {
                text = "팔로잉한 유저가 없습니다."
                textSize = 16f
            })
            return
        }

        val inflater = LayoutInflater.from(this)
        for (followEmail in followingList) {
            firestore.collection("users")
                .whereEqualTo("email", followEmail)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val doc = querySnapshot.documents.firstOrNull() ?: return@addOnSuccessListener
                    val followUser = doc.toObject(UserModel::class.java) ?: return@addOnSuccessListener
                    val followType = followUser.type ?: "kakao"
                    val followDocPath = "$followEmail&$followType"

                    val cardView = inflater.inflate(R.layout.follow_card, binding.followingContainer, false)
                    cardView.findViewById<TextView>(R.id.user_email).text = followEmail
                    cardView.findViewById<TextView>(R.id.review_count).text =
                        "리뷰 ${followUser.reviews?.size ?: 0}개"

                    val unfollowBtn = cardView.findViewById<android.widget.ImageButton>(R.id.follow_button)
                    unfollowBtn.setImageResource(R.drawable.cancel)
                    unfollowBtn.setOnClickListener {
                        val myDocPath = "${UserManager.currentUser?.email}&${UserManager.currentUser?.type}"
                        unfollowUser(myDocPath, followDocPath, followEmail)
                    }

                    binding.followingContainer.addView(cardView)
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

    private fun deleteAccount() {
        val email = UserManager.currentUser?.email
        val type = UserManager.currentUser?.type

        if (email == null || type == null) {
            Toast.makeText(this, "로그인 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val docPath = "$email&$type"
        val userDocRef = firestore.collection("users").document(docPath)

        firestore.runBatch { batch ->
            batch.update(userDocRef, mapOf("reviews" to FieldValue.delete()))
        }.addOnSuccessListener {
            userDocRef.delete().addOnSuccessListener {
                getSharedPreferences("autoLogin", Context.MODE_PRIVATE).edit().clear().apply()
                Toast.makeText(this, "계정이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SignInActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "계정 삭제 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "리뷰 삭제 실패: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}
