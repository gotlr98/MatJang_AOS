package com.example.matjang_aos

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

enum class Type {
    kakao, Guest
}

data class UserModel(

    val email: String = "",
    val type: Type = Type.kakao,
    val reviews: List<Review> = emptyList()
)