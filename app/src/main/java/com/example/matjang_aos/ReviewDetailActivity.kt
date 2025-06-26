package com.example.matjang_aos

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.matjang_aos.databinding.ActivityReviewDetailBinding // ✅ ViewBinding import
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ReviewDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewDetailBinding // ✅ ViewBinding 변수 선언
    private lateinit var adapter: ReviewAdapter
    private val reviewList = mutableListOf<Review>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewDetailBinding.inflate(layoutInflater) // ✅ ViewBinding 초기화
        setContentView(binding.root) // ✅ 레이아웃 설정

        adapter = ReviewAdapter(reviewList)
        binding.reviewRecyclerView.layoutManager = LinearLayoutManager(this) // ✅ 바인딩된 뷰 사용
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
                    val review = doc.toObject(Review::class.java)
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
}
