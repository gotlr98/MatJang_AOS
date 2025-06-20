package com.example.matjang_aos

import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FindFollowerView : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserEmail = UserManager.currentUser?.email
    private val currentUserType = UserManager.currentUser?.type
    private val docPath = "$currentUserEmail&$currentUserType"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_follower_view)

        container = findViewById(R.id.follower_container)
        swipeRefreshLayout = findViewById(R.id.swipeRefresh)

        val toolbar = findViewById<Toolbar>(R.id.custom_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기 버튼

        swipeRefreshLayout.setOnRefreshListener {
            loadUsers()
        }

        loadUsers()
    }

    private fun loadUsers() {
        swipeRefreshLayout.isRefreshing = true
        container.removeAllViews()

        if (currentUserEmail == null || currentUserType == null) {
            Toast.makeText(this, "유저 정보가 유효하지 않습니다.", Toast.LENGTH_SHORT).show()
            swipeRefreshLayout.isRefreshing = false
            return
        }

        val currentUserRef = firestore.collection("users").document(docPath)

        currentUserRef.get().addOnSuccessListener { doc ->
            val followingList = doc.get("following") as? List<*> ?: emptyList<String>()

            firestore.collection("users")
                .get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        val user = doc.toObject(UserModel::class.java)
                        val email = user.email ?: continue
                        if (email == currentUserEmail) continue

                        val reviewCount = user.reviews?.size ?: 0
                        val card = layoutInflater.inflate(R.layout.follow_card, container, false)
                        val emailView = card.findViewById<TextView>(R.id.user_email)
                        val reviewView = card.findViewById<TextView>(R.id.review_count)
                        val followButton = card.findViewById<ImageButton>(R.id.follow_button)

                        emailView.text = email
                        reviewView.text = "리뷰 ${reviewCount}개"

                        val isFollowing = followingList.contains(email)
                        followButton.setImageResource(
                            if (isFollowing) R.drawable.cancel else R.drawable.ic_person_add
                        )

                        followButton.setOnClickListener {
                            toggleFollow(email, isFollowing) {
                                loadUsers() // refresh only after follow/unfollow
                            }
                        }

                        container.addView(card)
                    }
                    swipeRefreshLayout.isRefreshing = false
                }
                .addOnFailureListener {
                    Toast.makeText(this, "유저 목록 로딩 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                    swipeRefreshLayout.isRefreshing = false
                }
        }.addOnFailureListener {
            Toast.makeText(this, "내 정보 조회 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun toggleFollow(targetEmail: String, isCurrentlyFollowing: Boolean, onComplete: () -> Unit) {
        val usersRef = firestore.collection("users")

        usersRef.whereEqualTo("email", targetEmail)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(this, "대상 유저 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val targetDoc = querySnapshot.documents[0]
                val targetType = targetDoc.getString("type") ?: "kakao"
                val targetDocPath = "$targetEmail&$targetType"

                val currentUserRef = usersRef.document(docPath)
                val targetUserRef = usersRef.document(targetDocPath)

                firestore.runBatch { batch ->
                    if (isCurrentlyFollowing) {
                        batch.update(currentUserRef, "following", FieldValue.arrayRemove(targetEmail))
                        batch.update(targetUserRef, "follower", FieldValue.arrayRemove(currentUserEmail))
                    } else {
                        batch.update(currentUserRef, "following", FieldValue.arrayUnion(targetEmail))
                        batch.update(targetUserRef, "follower", FieldValue.arrayUnion(currentUserEmail))
                    }
                }.addOnSuccessListener {
                    val action = if (isCurrentlyFollowing) "언팔로우" else "팔로우"
                    Toast.makeText(this, "$targetEmail $action 완료!", Toast.LENGTH_SHORT).show()
                    onComplete()
                }.addOnFailureListener {
                    Toast.makeText(this, "$targetEmail 처리 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "유저 정보 조회 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // 현재 액티비티 종료 = 뒤로가기
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
