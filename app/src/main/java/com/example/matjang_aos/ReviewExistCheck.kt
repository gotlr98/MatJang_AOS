package com.example.matjang_aos

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object ReviewUtil {
    fun fetchReviewsForPlace(placeName: String, callback: (List<Review>) -> Unit) {
        val sanitizedPlaceName = placeName.replace("/", "_")
        val db = Firebase.firestore

        db.collection("review")
            .document(sanitizedPlaceName)
            .collection("userReviews")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val reviews = querySnapshot.documents.mapNotNull { it.toObject(Review::class.java) }
                callback(reviews)
            }
            .addOnFailureListener { e ->
                Log.e("ReviewUtil", "리뷰 가져오기 실패", e)
                callback(emptyList())
            }
    }
}
