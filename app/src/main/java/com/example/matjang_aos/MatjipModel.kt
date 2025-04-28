package com.example.matjang_aos

data class CategorySearchResponse(
    val documents: List<Matjip>
)

data class Matjip(
    val place_name: String,
    val place_url: String,
    val category_name: String,
    val road_address_name: String,
    val address_name: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double
)
