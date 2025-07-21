package com.example.matjang_aos

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.example.matjang_aos.ReviewModel

object ReviewRepository {

    private val db = FirebaseFirestore.getInstance()

    fun fetchSortedReviews(
        userEmail: String,
        sortBy: String = "latest", // "latest" or "rating"
        filterCategory: String? = null,
        onComplete: (List<ReviewModel>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        db.collection("users")
            .document(userEmail)
            .collection("reviews")
            .get()
            .addOnSuccessListener { result ->
                val reviews = result.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        ReviewModel(
                            address = data["address"] as? String ?: "",
                            comment = data["comment"] as? String ?: "",
                            placeName = data["placeName"] as? String ?: "",
                            rate = (data["rate"] as? Number)?.toDouble() ?: 0.0,
                            user_email = data["user_email"] as? String ?: "",
                            category = data["category"] as? String ?: "",
                            timestamp = (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()
                        )
                    } catch (e: Exception) {
                        Log.e("ReviewRepository", "Error parsing review: ${e.message}")
                        null
                    }
                }.let { list ->
                    // 카테고리 필터
                    val filtered = if (!filterCategory.isNullOrEmpty()) {
                        list.filter { it.category == filterCategory }
                    } else {
                        list
                    }

                    // 정렬
                    when (sortBy) {
                        "latest" -> filtered.sortedByDescending { it.timestamp }
                        "rating" -> filtered.sortedByDescending { it.rate }
                        else -> filtered
                    }
                }

                onComplete(reviews)
            }
            .addOnFailureListener { exception ->
                Log.e("ReviewRepository", "Error fetching reviews: $exception")
                onError(exception)
            }
    }
}
