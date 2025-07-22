package com.example.matjang_aos

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.matjang_aos.databinding.ActivityReviewDetailBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ReviewDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewDetailBinding
    private lateinit var adapter: ReviewAdapter
    private val reviewList = mutableListOf<ReviewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ReviewAdapter(reviewList, onReportClick = { review ->
            showReportDialog(review)
        })

        binding.reviewRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.reviewRecyclerView.adapter = adapter

        val place = intent.getSerializableExtra("place") as? Matjip
        if (place == null) {
            Toast.makeText(this, "장소 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchReviews(place.placeName)
    }

    private fun fetchReviews(placeName: String) {
        val sanitizedPlaceName = placeName.replace("/", "_")
        val db = Firebase.firestore

        db.collection("review")
            .document(sanitizedPlaceName)
            .collection("userReviews")
            .get()
            .addOnSuccessListener { documents ->
                reviewList.clear()
                for (doc in documents) {
                    val review = doc.toObject(ReviewModel::class.java)
                    reviewList.add(review)
                }

                if (reviewList.isEmpty()) {
                    Toast.makeText(this, "작성된 리뷰가 없습니다.", Toast.LENGTH_SHORT).show()
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "리뷰 불러오기 실패", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showReportDialog(review: ReviewModel) {
        val dialog = android.app.AlertDialog.Builder(this)
        val input = android.widget.EditText(this).apply {
            hint = "신고 사유를 입력하세요"
        }

        dialog.setTitle("리뷰 신고")
        dialog.setView(input)
        dialog.setPositiveButton("제출") { _, _ ->
            val reason = input.text.toString().trim()
            if (reason.isNotEmpty()) {
                sendReportToFirestore(review, reason)
            } else {
                Toast.makeText(this, "신고 사유를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.setNegativeButton("취소", null)
        dialog.show()
    }

    private fun sendReportToFirestore(review: ReviewModel, reason: String) {
        val db = Firebase.firestore
        val docPath = review.placeName.replace("/", "_")
        val reportData = mapOf(
            "placeName" to review.placeName,
            "reviewer" to review.user_email,
            "reason" to reason,
            "comment" to review.comment,
            "rate" to review.rate,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("reports")
            .document(docPath)
            .collection("userReports")
            .add(reportData)
            .addOnSuccessListener {
                Toast.makeText(this, "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "신고 접수에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }
}
