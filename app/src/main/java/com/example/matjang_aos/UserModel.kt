package com.example.matjang_aos

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import kotlinx.android.parcel.Parcelize

enum class Type {
    Kakao, Guest
}

@Parcelize
data class UserModel(

    val email: String?,
    val type: Type


) : Parcelable