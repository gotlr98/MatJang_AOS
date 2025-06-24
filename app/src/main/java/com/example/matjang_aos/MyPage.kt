package com.example.matjang_aos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MyPage : AppCompatActivity() {
    private lateinit var reviewContainer: LinearLayout
    private lateinit var followingContainer: LinearLayout
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        initViews()
        setupToolbar()
        setupButtons()

        val email = UserManager.currentUser?.email
        val type = UserManager.currentUser?.type

        if (email == null || type == null) {
            Toast.makeText(this, "로그인 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val docPath = "$email&$type"
        loadUserData(docPath)
    }

    // 초기 View 바인딩
    private fun initViews() {
        reviewContainer = findViewById(R.id.review_container)
        followingContainer = findViewById(R.id.following_container)
    }

    // 툴바 설정
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.custom_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "마이페이지"
    }

    // 버튼 설정
    private fun setupButtons() {
        findViewById<ImageButton>(R.id.btn_add_follow).setOnClickListener {
            startActivity(Intent(this, FindFollowerView::class.java))
        }

        findViewById<Button>(R.id.btn_delete_account).setOnClickListener {
            deleteAccount()
        }
    }

    // 사용자 데이터 로딩
    private fun loadUserData(docPath: String) {
        firestore.collection("users")
            .document(docPath)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(UserModel::class.java)
                displayUserReviews(user?.reviews)
                displayFollowingUsers(user?.following ?: emptyList()) // ✅ 정리됨
            }
    }

    // 리뷰 보여주기
    private fun displayUserReviews(reviews: List<Review>?) {
        if (reviews.isNullOrEmpty()) {
            reviewContainer.addView(TextView(this).apply {
                text = "작성한 리뷰가 없습니다."
                textSize = 16f
            })
            return
        }

        val inflater = LayoutInflater.from(this)
        for (review in reviews) {
            val cardView = inflater.inflate(R.layout.review_card, reviewContainer, false)
            cardView.findViewById<TextView>(R.id.place_name).text = "📍 ${review.placeName}"
            cardView.findViewById<TextView>(R.id.rate_text).text = "⭐ 평점: ${review.rate}"
            cardView.findViewById<TextView>(R.id.comment_text).text = "💬 ${review.comment}"
            reviewContainer.addView(cardView)
        }
    }

    // 팔로잉 유저 보여주기
    private fun displayFollowingUsers(followingList: List<String>) {
        if (followingList.isEmpty()) {
            followingContainer.addView(TextView(this).apply {
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

                    val cardView = inflater.inflate(R.layout.follow_card, followingContainer, false)
                    cardView.findViewById<TextView>(R.id.user_email).text = followEmail
                    cardView.findViewById<TextView>(R.id.review_count).text = "리뷰 ${followUser.reviews?.size ?: 0}개"

                    val unfollowBtn = cardView.findViewById<ImageButton>(R.id.follow_button)
                    unfollowBtn.setImageResource(R.drawable.cancel)
                    unfollowBtn.setOnClickListener {
                        unfollowUser("${
                            UserManager.currentUser?.email
                        }&${UserManager.currentUser?.type}", followDocPath, followEmail)
                    }

                    followingContainer.addView(cardView)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
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
}
