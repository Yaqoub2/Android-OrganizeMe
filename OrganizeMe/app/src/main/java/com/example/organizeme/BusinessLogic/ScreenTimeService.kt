package com.example.organizeme.BusinessLogic
//package:com.example.organizeme  tag:myworker tag:hhh tag:ScreenTimeService tag:firstHour tag:first


import android.Manifest
import android.app.AlertDialog
import android.app.ForegroundServiceStartNotAllowedException
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock.uptimeMillis
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.MutableLiveData
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.organizeme.DB.Dao
import com.example.organizeme.DB.Day
import com.example.organizeme.DB.Hour
import com.example.organizeme.DB.Pickup
import com.example.organizeme.DB.Pickup_Database
import com.example.organizeme.R
import com.example.organizeme.Reduction.DF_Algorithm
import com.example.organizeme.Reduction.IF_Algorithm
import com.example.organizeme.Restriction.AlertDay
import com.example.organizeme.Restriction.AlertHour
import com.example.organizeme.Restriction.Vibrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min


class ScreenTimeService : Service() {
    companion object {
        val ST = MutableLiveData<Long>()
        val HST = MutableLiveData<Long>()
    }

    //Restriction
    private var dayL = 36_000_000L
    private var dayRflag = true
    private lateinit var myWindow: AlertDay
    private lateinit var hourWindow: AlertHour
    private var virbate = Vibrator()

    //DB
    private lateinit var db: Pickup_Database
    private lateinit var dao: Dao

    //coroutines
    private val job = SupervisorJob()
    private val cScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1) + job)

    //------------------------------------
    private val TAG = "ScreenTimeService"
    private val CHANNEL_ID = "ScreenTimeChannel"
    private val NOTIFICATION_ID = 1

    private lateinit var managerComp: NotificationManagerCompat

    //------------Timer things-------------\\
    private var timer = Timer()


    private var startTime = 0L
    private var accum = 0L

    private var hourAccum = 0L


    lateinit var sharedPref: SharedPreferences
    lateinit var editor: SharedPreferences.Editor
    private var noteFlag = true
    var screenFlag = false
    private var d = LocalDate.now().toString()
    private var concurrentFlag = true

    private lateinit var previousDate: LocalDate
    private lateinit var previousHour: LocalDateTime
    private var dateString: String? = null
    private var hourString: String? = null

    //managers
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var wManager: WindowManager

    //----------broadcast receiver----------------\\
    private lateinit var brReceiver: BroadcastReceiver

    //------------------Timer task-------------------\\
    private inner class TimeTask : TimerTask() {
        override fun run() {
            ST.postValue(accum)
            HST.postValue(hourAccum)
            //---------------------accum=0 each day---------------------\\
            //after it
            if (previousDate.isBefore(LocalDate.now()) && concurrentFlag) {
                d = previousDate.toString()
                myWindow.dismiss()
                //db each day
                //end of day
                //todo: have to update DF,ST
                cScope.launch {
                    val day = dao.getDay(previousDate.toString())
                    day.ST = accum
                    //AA
                    var stringAA = dao.getlastAlarmDayAfter()
                    var lastAlarm = 2
                    if (stringAA != null) {
                        lastAlarm = Period.between(LocalDate.parse(stringAA), previousDate).days
                    } else {
                        stringAA = dao.getlastAlarmDayNull()
                        if (stringAA != null) lastAlarm =
                            Period.between(LocalDate.parse(stringAA), previousDate).days + 1
                    }
                    //DF
                    val df = DF_Algorithm.calcDF(day.DF, accum, day.DL, lastAlarm)
                    dao.updateDay(day.copy(DF = df, ST = accum))

                    editor.putString("prevDate", LocalDate.now().toString())
                    editor.putLong("accum", 0)
                    editor.apply()
                    accum = 0
                    dayRflag = true
                    Log.i("first", "accum should be 0, and previous: $previousDate")
                    previousDate = LocalDate.now()
                    //start of new day

                    //todo : insert new day with DL
                    val avgST = dao.getAvgST()
                    val DL: Long = avgST - (avgST * df).toLong()
                    dayL = DL
                    val day1 = Day(dayID = previousDate.toString(), DL = DL, DF = df)
                    dao.insertDay(day1)
                    concurrentFlag = true
                }
                concurrentFlag = false
            }
            //------------------------hourly accum = 0 each hour----------------------------\\
            //hourly accum after it\\
            Log.i("firstHour", "hour: $previousHour")
            if (previousHour.atZone(ZoneId.systemDefault()).toInstant()
                    .toEpochMilli() / 3_600_000 < System.currentTimeMillis() / 3_600_000
            ) {
                //TODO db each hour
                Log.i("23", "this is outside of coroutine ${d}")
                //end of hour
                cScope.launch {
                    Log.i("23", "this is inside of coroutine ${d}")
                    updateHourFocus(d, previousHour.hour)
                    d = previousDate.toString()

                    editor.putString("prevHour", LocalDateTime.now().toString())
                    editor.apply()
                    hourAccum = 0
                    Log.i("firstHour", "hour should be 0, and previous: $previousHour")
                    previousHour = LocalDateTime.now()
                }
            }


            //----------------------------------------------------------\\
            var endTime: Long = uptimeMillis()
            accum += endTime - startTime
            hourAccum += endTime - startTime
            startTime = endTime
            var seconds = (accum / 1000).toInt()
            var minutes = seconds / 60
            var hours = minutes / 60
            hours %= 24
            minutes %= 60
            seconds = seconds % 60
            //notify
            if (ActivityCompat.checkSelfPermission(
                    baseContext, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            if (noteFlag == true) {
                managerComp.notify(
                    NOTIFICATION_ID, createNotification()
                        .setContentText(String.format("%02d:%02d:%02d", hours, minutes, seconds))
                        .build()
                )
            }
        }
    }

    //---------br reciever Class-------------------\\
    private inner class brClass : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (Intent.ACTION_USER_PRESENT == intent.action) {
                startTime = uptimeMillis()
                Log.d("hhh", "SCREEN_ON is received")
                // create timer
                timer.cancel()
                timer.purge()
                timer = Timer()
                timer.schedule(TimeTask(), 0, 1000)
                screenFlag = true
                cScope.launch {
                    // TODO: insert new Pickup
                    dao.insertDay(Day(dayID = previousDate.toString()))
                    dao.insertHour(
                        Hour(
                            dayID = previousDate.toString(),
                            hourID = previousHour.hour
                        )
                    )
                    val startPick =
                        Pickup(dayID = previousDate.toString(), hourID = previousHour.hour)
                    dao.insertPU(startPick)
                    Log.i("ccc", "service screen_on pu: ${Thread.currentThread().name}")
                    Log.i(
                        "ccc",
                        "service screen_on context: ${currentCoroutineContext().toString()}"
                    )
                }
                Log.i("ccc", "service screen_on pu at time: ${LocalTime.now()} after runBlocking")

            } else if (Intent.ACTION_SCREEN_OFF == intent.action && (screenFlag)) {
                if (keyguardManager.isKeyguardLocked) {
                    cScope.launch {
                        // Todo: update Pickup end time
                        val pickup = dao.getLastPU()
                        if (pickup != null && pickup.startPU < System.currentTimeMillis()) {
                            pickup.endPU = System.currentTimeMillis()
                            dao.updatePickUP(pickup)

                        }
                    }
                    cScope.launch(Dispatchers.Default) {
                        val HM = dao.getHourBPHM(previousHour.hour)
                        hourWindow.show(
                            myWindow,
                            HM,
                            db,
                            dao,
                            cScope,
                            previousDate.toString(),
                            previousHour.hour
                        )
                    }
                    screenFlag = false
                    //cancel timer
                    timer.cancel()
                    timer.purge()
                    editor.putLong("accum", accum)
                    editor.apply()
                    Log.d("hhh", "SCREEN_off is received")
//                    virbate.vibratePhone(this@ScreenTimeService, 1000L)
                }
            } else if (Intent.ACTION_DATE_CHANGED == intent.action) {
                cScope.launch {
                    delay(1000)
                    // TODO: update Hour_Blueprint
                    updateHourBlueprint()
                }
            } else if (Intent.ACTION_TIME_TICK == intent.action) {
                //day restriction
                if (accum > dayL && dayRflag) {
                    myWindow.show(db, dao, cScope, previousDate.toString())
                    dayRflag = false
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        db = Pickup_Database.getDatabase(this)
        dao = db.dao()
        //managing tools
        managerComp = NotificationManagerCompat.from(this)
        wManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        myWindow = AlertDay(this, wManager)
        hourWindow = AlertHour(this, wManager)
        sharedPref = getSharedPreferences("MyPref", MODE_PRIVATE)
        editor = sharedPref.edit()
        startForeground()

        Log.i(TAG, "On Create " + " - serviceID *")

        //first time day accum
        dateString = sharedPref.getString("prevDate", null)
        Log.i("first", "date: $dateString")
        if (dateString == null) {
            editor.putString("prevDate", LocalDate.now().toString())
            editor.apply()
            Log.i("first", "first date: ${LocalDate.now()}")
        }
        //after first day initialize
        dateString = sharedPref.getString("prevDate", LocalDate.now().toString())
        previousDate = LocalDate.parse(dateString)

        //first hourly accum
        hourString = sharedPref.getString("prevHour", null)
        if (hourString == null) {
            editor.putString("prevHour", LocalDateTime.now().toString())
            editor.apply()
            Log.i("firstHour", "after hour initialize : ${LocalTime.now()}")
        }
        //after first hour initialize
        hourString = sharedPref.getString("prevHour", LocalDateTime.now().toString())
        previousHour = LocalDateTime.parse(hourString)

        //time counter
        accum = sharedPref.getLong("accum", accum)

        keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        //first time timer
        // TODO:
        cScope.launch {
            val dayx = dao.getLastDay()
            dayL = dayx?.DL ?: 36_000_000
            dao.insertDay(Day(dayID = previousDate.toString(), DL = (dayx?.DL ?: 36_000_000)))
            Log.i("ccc", "service created day context: ${currentCoroutineContext()}")
        }

        if (!keyguardManager.isKeyguardLocked) {
            startTime = uptimeMillis()
            timer.cancel()
            timer.purge()
            screenFlag = true
            timer = Timer()
            timer.schedule(TimeTask(), 0, 1000)
            cScope.launch {
                // TODO:
                dao.insertHour(Hour(dayID = previousDate.toString(), hourID = previousHour.hour))
                dao.insertPU(Pickup(dayID = previousDate.toString(), hourID = previousHour.hour))
                Log.i("ccc", "service created pu at time: ${LocalTime.now()}")
            }
        }
        //broadcast receiver for timer start and stop
        brReceiver = brClass()
        //register br
        var intentF: IntentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
        }
        registerReceiver(brReceiver, intentF)
        //Worker to start Service
        val startWorkRequest = PeriodicWorkRequestBuilder<ServiceWorker>(15, TimeUnit.MINUTES)
            .addTag("startWorkRequest")
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "startWorkRequest",
            ExistingPeriodicWorkPolicy.KEEP,
            startWorkRequest
        )

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, startId.toString() + " - serviceID *")
        if (intent?.action == "ACTION_STOP_SERVICE") {
            Log.i(TAG, startId.toString() + " selfstop")
            noteFlag = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            //love it
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        editor.putLong("accum", accum)
        editor.apply()
        timer.cancel()
        timer.purge()
        cScope.cancel()
        myWindow.dismiss()
        hourWindow.dismiss()
        unregisterReceiver(brReceiver)
//        singleThreadExecutor.close()
        Log.i(TAG, "On Destroy")

    }

    private fun updateHourBlueprint() {
        val hours_bp = dao.getHoursBP()
        for (hour in hours_bp) {
            dao.updateAvgFocus(hour.hours_B_P)
            val hour_bp = dao.getHourBP(hour.hours_B_P)
            //AA
            var hAlarmString = dao.getlastAlarmHourAfter(hour_bp.hours_B_P)
            var hLastAlarm = 2
            if (hAlarmString != null) {
                hLastAlarm = Period.between(LocalDate.parse(hAlarmString), previousDate).days
            } else {
                hAlarmString = dao.getlastAlarmHourNull()
                if (hAlarmString != null) hLastAlarm =
                    Period.between(LocalDate.parse(hAlarmString), previousDate).days + 1
            }
            //focus
            var focus = dao.getFocusByHour(hour_bp.hours_B_P)
            var iF = hour_bp.IF
            if (focus != 0L) {
                iF = IF_Algorithm.calcIF(hour_bp.IF, focus, hour_bp.hMin, hLastAlarm)
            }
            hour_bp.IF = iF
            hour_bp.hMin = hour_bp.avgFocus + (hour_bp.IF * hour_bp.avgFocus).toLong()
            dao.updateHourBP(hour_bp)
        }
    }

    private fun updateHourFocus(dayID: String, hourID: Int) {
        val hourPickups: List<Pickup> = dao.getHourPickups(dayID, hourID)
        Log.i("ppp", "hourPickups: $hourPickups")
        if (hourPickups != null && hourPickups.size > 1) {
            var count = 0
            var focus = 0L
            for (i in 0..hourPickups.size - 2) {
                focus += (hourPickups[i + 1].startPU % 3_600_000) - (hourPickups[i].endPU % 3_600_000)
                count++
            }
            focus = abs(focus / count)
            focus = min(focus, 1_500_000)
            dao.updateHourByFocus(dayID, hourID, focus)
        }
    }

    private fun startForeground() {
        try {
            val manager = createNotificationChannel()
            // Create the notification to display while the service is running
            var builder = createNotification()
            val notification = builder.build()
            //runtime permission
            ServiceCompat.startForeground(
                /* service = */ this,
                /* id = */ NOTIFICATION_ID, // Cannot be 0
                /* notification = */ notification,
                /* foregroundServiceType = */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                },
            )

        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {
                // App not in a valid state to start foreground service
                // (e.g. started from bg)
                Toast.makeText(
                    this,
                    "ForegroundServiceStartNotAllowedException",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
    }

    private fun createNotificationChannel(): NotificationManager {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Time Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
        return manager
    }

    private fun createNotification(): NotificationCompat.Builder {
        val intent = Intent(this, ScreenTimeService::class.java)
        intent.action = "ACTION_STOP_SERVICE"
        val pend = PendingIntent.getService(
            this, 0,
            intent, PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Time Service")
            .setContentText("Tracking screen time")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSmallIcon(R.drawable.baseline_access_time_24)
            .setOngoing(true)
            .addAction(0, "stop", pend)
            .setSilent(true)

        return builder
    }

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
