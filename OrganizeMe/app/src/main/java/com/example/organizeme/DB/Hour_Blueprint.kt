package com.example.organizeme.DB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Hour_Blueprint(
    @PrimaryKey
    val hours_B_P: Int,
    var IF:Double = 0.005,
    var avgFocus: Long = 60000,
    var hMin: Long = 60000,
)