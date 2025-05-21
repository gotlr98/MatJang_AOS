package com.example.matjang_aos

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object ReviewUtil {
    fun checkIfUserReviewed(userEmail: String, placeName: String, callback: (Boolean) -> Unit) {
        val sanitizedPlaceName = placeName.replace("/", "_")
        val db = Firebase.firestore

        Log.d("email", "${userEmail}&kakao")

        db.collection("users")
            .document("${userEmail}&kakao")
            .collection("review")
            .document(sanitizedPlaceName)
            .get()
            .addOnSuccessListener { document ->
                callback(document.exists())
            }
            .addOnFailureListener {
                callback(false)
            }
    }
}