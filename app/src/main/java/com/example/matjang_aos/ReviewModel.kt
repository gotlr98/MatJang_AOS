package com.example.matjang_aos

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Review(
    val placeName: String = "",
    val rate: Double = 0.0,
    val comment: String = "",
    val user_email: String = "",
    val address: String = "",
): Parcelable