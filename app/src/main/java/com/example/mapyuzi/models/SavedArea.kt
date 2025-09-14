package com.example.mapyuzi.models

data class SavedArea(
    val id: Int = 0,
    val name: String,
    val points: String, // JSON formatda saqlanadi: "[lat1,lng1],[lat2,lng2],..."
    val area: String
)
