package com.example.matjang_aos

import android.util.Log
import com.example.matjang_aos.ReviewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.util.Date

object ReviewUtil {

    private val db = Firebase.firestore

    // ✅ 이미 작성한 리뷰인지 체크
    fun checkIfUserReviewed(
        userEmail: String,
        userType: String,
        placeName: String,
        callback: (Boolean) -> Unit
    ) {
        val sanitizedPlaceName = placeName.replace("/", "_")
        val docID = "$userEmail&$userType"

        db.collection("review")
            .document(sanitizedPlaceName)
            .collection("userReviews")
            .document(docID)
            .get()
            .addOnSuccessListener { document ->
                callback(document.exists())
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    // ✅ 정렬/필터 포함 리뷰 불러오기
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
                            address = data["address"] as? String ?: data["address_name"] as? String ?: "",
                            comment = data["comment"] as? String ?: "",
                            placeName = data["placeName"] as? String ?: "",
                            rate = (data["rate"] as? Number)?.toDouble() ?: 0.0,
                            user_email = data["user_email"] as? String ?: "",
                            category = data["category"] as? String ?: "",
                            timestamp = (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                        )
                    } catch (e: Exception) {
                        Log.e("ReviewUtil", "Error parsing review: ${e.message}")
                        null
                    }
                }.let { list ->
                    // 필터
                    val filtered = if (!filterCategory.isNullOrEmpty()) {
                        list.filter { it.category == filterCategory }
                    } else list

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
                Log.e("ReviewUtil", "Error fetching reviews: $exception")
                onError(exception)
            }
    }
}
