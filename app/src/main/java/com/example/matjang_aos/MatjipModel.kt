package com.example.matjang_aos

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CategorySearchResponse(
    val documents: List<Matjip>
)

data class Matjip(
    @SerializedName("place_name") val placeName: String,
    @SerializedName("category_name") val category: String,
    @SerializedName("x") val longitude: Double,  // ← x는 경도
    @SerializedName("y") val latitude: Double,   // ← y는 위도
    @SerializedName("address_name") val address: String?,
) : Serializable
