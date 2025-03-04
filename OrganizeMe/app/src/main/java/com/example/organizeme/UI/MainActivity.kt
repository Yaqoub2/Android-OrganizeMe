package com.example.organizeme.UI


import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.organizeme.DB.Dao
import com.example.organizeme.DB.Pickup_Database
import com.example.organizeme.R
import com.example.organizeme.BusinessLogic.ScreenTimeService
import com.example.organizeme.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: Pickup_Database
    private lateinit var dao: Dao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //----------------------------permissions---------------------------\\
        //runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.SYSTEM_ALERT_WINDOW
                    ),
                    0
                )
            }
        }
        //permission for window
        if (!Settings.canDrawOverlays(this)) {
            val myIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(myIntent)
        }
        //start if not already started
        val intent1 = Intent(this, ScreenTimeService::class.java)
        if (!isServiceRunning(ScreenTimeService::class.java)) startForegroundService(intent1)
        changeFragement(DayFragment())


        binding.bottomNavigation.setOnItemSelectedListener {
            when(it.itemId){
                R.id.item_1 -> changeFragement(DayFragment())
                R.id.item_2 -> changeFragement(HourFragment())
                else -> {

                }
            }
            true
        }


    }
     fun changeFragement(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }



    @Suppress("DEPRECATION")
    fun <T> Context.isServiceRunning(service: Class<T>) =
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Integer.MAX_VALUE)
            .any {
                it.service.className == service.name
            }

}