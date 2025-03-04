package com.example.organizeme.DB

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity
data class Day(
    @PrimaryKey
    val dayID:String = LocalDate.now().toString(),
    val DF:Double = 0.01,
    val DL:Long = 36_000_000L, //10 hours
    var ST:Long = 0L,
    var aAlarm:Int = 0
)
