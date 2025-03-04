package com.example.organizeme.DB

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(
    foreignKeys = [ForeignKey(
        entity = Day::class,
        parentColumns = ["dayID"],
        childColumns = ["dayID"],
        onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE
    ), ForeignKey(
        entity = Hour::class,
        parentColumns = ["dayID","hourID"],
        childColumns = ["dayID","hourID"],
        onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE
    )]
)
data class Pickup(
    @PrimaryKey(autoGenerate = true)
    val pickUpID: Long = 0,
    val startPU: Long = System.currentTimeMillis(),
    var endPU: Long = startPU,
    val dayID: String = LocalDate.now().toString(),
    val hourID: Int = LocalTime.now().hour
)