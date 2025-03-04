package com.example.organizeme.DB

import androidx.room.Entity
import androidx.room.ForeignKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    primaryKeys = ["dayID", "hourID"], foreignKeys = [ForeignKey(
        entity = Day::class,
        parentColumns = ["dayID"],
        childColumns = ["dayID"],
        onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE
    )]
)
data class Hour(
    val dayID: String = LocalDate.now().toString(),
    val hourID: Int = LocalDateTime.now().hour,
    val focus: Long = 60000, //in milliseconds
    var h_aAlarm: Int = 0
)
