package com.example.organizeme.DB

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface Dao {
    //-----------------------------pickup queries-----------------------------\\
    @Insert(entity = Pickup::class, onConflict = OnConflictStrategy.IGNORE)
    fun insertPU(pickup: Pickup)

    @Query("SELECT * FROM pickup where pickUpID = (SELECT MAX(pickUpID) From Pickup)")
    fun getLastPU(): Pickup

    @Update
    fun updatePickUP(endPU: Pickup)

    //---------------------------------day queries------------------------------\\
    @Query("Select count(pickUpID) from Pickup where dayID = :dayID")
    fun getDayPU(dayID: String): Int

    @Insert(entity = Day::class, onConflict = OnConflictStrategy.IGNORE)
    fun insertDay(day: Day)

    @Update
    fun updateDay(day: Day)

    @Query("Select * from Day where dayID = :dayID")
    fun getDay(dayID: String): Day

    @Query("select dayID from Day where aAlarm > 0 order by dayID Desc limit 1")
    fun getlastAlarmDayAfter(): String

    @Query("select min(dayID) from Day limit 1")
    fun getlastAlarmDayNull(): String

    @Query("Select COALESCE( cast(AVG(ST) as INTEGER) , 36000000 ) from Day ")
    fun getAvgST(): Long

    @Query("SELECT * FROM Day where dayID = (SELECT MAX(dayID) From Day)" )
    fun getLastDay(): Day

    @Query("Select aAlarm from Day where dayID = :dayID")
    fun getDayAA(dayID: String): Long

    @Query( "Update Day set aAlarm = 1 where dayID = :dayID")
    fun updateDayAA(dayID: String)

    //--------------------------------Hour queries-------------------------------\\
    @Insert(entity = Hour::class, onConflict = OnConflictStrategy.IGNORE)
    fun insertHour(hour: Hour)

    @Update
    fun updateHour(hour: Hour)

    @Query("Update Hour set focus = :focus where dayID = :dayID AND hourID = :hourID")
    fun updateHourByFocus(dayID:String, hourID: Int, focus: Long)


    @Query("Select * from Pickup where dayID = :dayID AND hourID = :hourID order by pickUpID")
    fun getHourPickups(dayID:String, hourID: Int): List<Pickup>

    @Query("Select count(pickUpID) from Pickup where dayID = :dayID AND hourID = :hourID")
    fun getHourPU(hourID: Int, dayID: String): Int

    @Query("Select * from Hour where dayID = :dayID")
    fun getDayHours(dayID:String): List<Hour>

    @Query("select dayID from Hour where h_aAlarm > 0 and hourID = :hourID order by dayID Desc limit 1")
    fun getlastAlarmHourAfter(hourID: Int): String

    @Query("select min(dayID) from Hour limit 1")
    fun getlastAlarmHourNull(): String

    @Query( "Update Hour set h_aAlarm = 1 where dayID = :dayID and hourID = :hourID")
    fun updateHourAA(dayID: String, hourID: Int)

    @Query("select h_aAlarm from Hour where dayID= :dayID and hourID = :hourID")
    fun getHourAA(dayID: String,hourID: Int): Long

//    @Query("select COALESCE( (select dayID from Hour where h_aAlarm > 0 and hourID = :hourID order by dayID Desc limit 1) , (select min(dayID) from Hour limit 1 ) )")
//    fun getlastAlarmHour(hourID: Int): String

    @Query("select focus from Hour where hourID = :hourID order by dayID Desc limit 1")
    fun getFocusByHour(hourID: Int): Long
    //-------------------------------Hour_B_P queries-----------------------------\\
    @Query("Select * from Hour_Blueprint")
    fun getHoursBP(): List<Hour_Blueprint>

    @Query("update Hour_Blueprint set avgFocus = COALESCE((select cast(AVG(focus) as INTEGER) from Hour where hourID = :hourID), 60000 ) where hours_B_P = :hourID")
    fun updateAvgFocus(hourID: Int)

    @Query( "Select * from Hour_Blueprint where hours_B_P = :hourID")
    fun getHourBP(hourID: Int): Hour_Blueprint

    @Update
    fun updateHourBP(hourBP: Hour_Blueprint)

    @Query("select hMin from Hour_Blueprint where hours_B_P = :hourID")
    fun getHourBPHM(hourID: Int): Long

}