package com.example.matjang_aos

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object ReviewUtil {
    fun checkIfUserReviewed(
        userEmail: String,
        placeName: String,
        onResult: (Boolean) -> Unit
    ) {
        val db = Firebase.firestore
        val docRef = db.collection("users")
            .document("$userEmail&Kakao")
            .collection("review")
            .document(placeName)

        docRef.get()
            .addOnSuccessListener { document ->
                onResult(document.exists())  // true면 작성한 것
            }
            .addOnFailureListener { e ->
                Log.e("ReviewCheck", "리뷰 확인 실패: ${e.message}")
                onResult(false)
            }
    }
}