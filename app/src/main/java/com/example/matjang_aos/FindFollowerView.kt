package com.example.matjang_aos

import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FindFollowerView : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserEmail = UserManager.currentUser?.email
    private val currentUserType = UserManager.currentUser?.type
    private val docPath = "$currentUserEmail&$currentUserType"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_follower_view)
        container = findViewById(R.id.follower_container)

        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val user = doc.toObject(UserModel::class.java)
                    val email = user.email ?: continue
                    if (email == currentUserEmail) continue

                    val reviewCount = user.reviews?.size ?: 0
                    val card = layoutInflater.inflate(R.layout.follow_card, container, false)
                    card.findViewById<TextView>(R.id.user_email).text = email
                    card.findViewById<TextView>(R.id.review_count).text = "리뷰 ${reviewCount}개"

                    val followButton = card.findViewById<ImageButton>(R.id.follow_button)
                    followButton.setOnClickListener {
                        followUser(email)
                    }

                    container.addView(card)
                }
            }
    }

    private fun followUser(targetEmail: String) {
        val targetDocPath = "$targetEmail&Kakao" // 필요 시 타입도 고려
        val currentUserRef = firestore.collection("users").document(docPath)
        val targetUserRef = firestore.collection("users").document(targetDocPath)

        firestore.runBatch { batch ->
            batch.update(currentUserRef, "following", FieldValue.arrayUnion(targetEmail))
            batch.update(targetUserRef, "follower", FieldValue.arrayUnion(currentUserEmail))
        }.addOnSuccessListener {
            Toast.makeText(this, "$targetEmail 팔로우 완료!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "팔로우 실패: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}