package com.example.matjang_aos

import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.matjang_aos.databinding.ActivityFindFollowerBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FindFollowerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFindFollowerBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserEmail = UserManager.currentUser?.email
    private val currentUserType = UserManager.currentUser?.type
    private val docPath = "$currentUserEmail&$currentUserType"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFindFollowerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.customToolbar.customToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.swipeRefresh.setOnRefreshListener { loadUsers() }
        loadUsers()
    }

    private fun loadUsers() {
        binding.swipeRefresh.isRefreshing = true
        binding.followerContainer.removeAllViews()

        if (currentUserEmail.isNullOrBlank() || currentUserType == null) {
            Toast.makeText(this, "유저 정보가 유효하지 않습니다.", Toast.LENGTH_SHORT).show()
            binding.swipeRefresh.isRefreshing = false
            return
        }

        val currentUserRef = firestore.collection("users").document(docPath)

        currentUserRef.get().addOnSuccessListener { doc ->
            val followingList = doc.get("following") as? List<*> ?: emptyList<String>()

            firestore.collection("users").get()
                .addOnSuccessListener { documents ->
                    documents.forEach { userDoc ->
                        val user = userDoc.toObject(UserModel::class.java)
                        val email = user.email ?: return@forEach
                        if (email == currentUserEmail) return@forEach

                        val reviewCount = user.reviews?.size ?: 0
                        val card = layoutInflater.inflate(R.layout.follow_card, binding.followerContainer, false)

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
                            toggleFollow(email, isFollowing) { loadUsers() }
                        }

                        binding.followerContainer.addView(card)
                    }

                    binding.swipeRefresh.isRefreshing = false
                }
                .addOnFailureListener {
                    showToast("유저 목록 로딩 실패: ${it.message}")
                    binding.swipeRefresh.isRefreshing = false
                }
        }.addOnFailureListener {
            showToast("내 정보 조회 실패: ${it.message}")
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun toggleFollow(targetEmail: String, isFollowing: Boolean, onComplete: () -> Unit) {
        val usersRef = firestore.collection("users")

        usersRef.whereEqualTo("email", targetEmail).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    showToast("대상 유저 정보를 찾을 수 없습니다.")
                    return@addOnSuccessListener
                }

                val targetDoc = querySnapshot.documents[0]
                val targetType = targetDoc.getString("type") ?: "kakao"
                val targetDocPath = "$targetEmail&$targetType"

                val currentUserRef = usersRef.document(docPath)
                val targetUserRef = usersRef.document(targetDocPath)

                firestore.runBatch { batch ->
                    if (isFollowing) {
                        batch.update(currentUserRef, "following", FieldValue.arrayRemove(targetEmail))
                        batch.update(targetUserRef, "follower", FieldValue.arrayRemove(currentUserEmail))
                    } else {
                        batch.update(currentUserRef, "following", FieldValue.arrayUnion(targetEmail))
                        batch.update(targetUserRef, "follower", FieldValue.arrayUnion(currentUserEmail))
                    }
                }.addOnSuccessListener {
                    val action = if (isFollowing) "언팔로우" else "팔로우"
                    showToast("$targetEmail $action 완료!")
                    onComplete()
                }.addOnFailureListener {
                    showToast("$targetEmail 처리 실패: ${it.message}")
                }
            }
            .addOnFailureListener {
                showToast("유저 정보 조회 실패: ${it.message}")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
}
