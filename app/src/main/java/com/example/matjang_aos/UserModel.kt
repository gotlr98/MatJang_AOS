package com.example.matjang_aos

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

enum class Type {
    Kakao, Guest
}
@Parcelize
data class Review(
    val rate: Double = 0.0,
    val review: String = "",
    val address: String = "",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val user: String = ""
): Parcelable

@Parcelize
data class UserModel(

    val email: String?,
    val type: Type,
    val reviews: @RawValue List<Review> = emptyList()

) : Parcelable