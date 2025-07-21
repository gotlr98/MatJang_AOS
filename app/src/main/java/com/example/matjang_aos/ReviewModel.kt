package com.example.matjang_aos

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Date

data class ReviewModel(
    val address: String,
    val comment: String,
    val placeName: String,
    val rate: Double,
    val user_email: String,
    val category: String,
    val timestamp: Date?
)