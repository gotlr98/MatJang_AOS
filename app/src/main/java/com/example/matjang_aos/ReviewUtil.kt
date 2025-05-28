package com.example.matjang_aos

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object ReviewUtil {
    fun checkIfUserReviewed(
        userEmail: String,
        userType: String,
        placeName: String,
        callback: (Boolean) -> Unit
    ) {
        val sanitizedPlaceName = placeName.replace("/", "_")
        val docID = "$userEmail&$userType"

        val db = Firebase.firestore
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
}

