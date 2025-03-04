package com.example.organizeme.UI

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.organizeme.DB.Dao
import com.example.organizeme.DB.Pickup_Database
import com.example.organizeme.BusinessLogic.ScreenTimeService
import com.example.organizeme.databinding.FragmentHourBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class HourFragment : Fragment() {
    private var _binding: FragmentHourBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: Pickup_Database
    private lateinit var dao: Dao


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHourBinding.inflate(inflater, container, false)

        // Initialize database and DAO here
        db = Pickup_Database.getDatabase(requireContext())
        dao = db.dao()

        val tvST: TextView = binding.HST
        val ST: LiveData<Long> = ScreenTimeService.HST
        ST.observe(viewLifecycleOwner, Observer {
            var seconds = (it / 1000).toInt()
            var minutes = seconds / 60
            minutes %= 60
            seconds = seconds % 60
            tvST.text = String.format("%02d:%02d", minutes, seconds)
        })

        lifecycleScope.launch(Dispatchers.IO) {
            delay(500)
            var day = dao.getDay(LocalDate.now().toString())
            var hourBP = dao.getHourBP(LocalTime.now().hour)

            var hMin: Long? = hourBP.hMin
            var nPU = dao.getHourPU(LocalTime.now().hour ,day.dayID)
            val focusH = dao.getFocusByHour(hourBP.hours_B_P)
            var Focus:Long = if(focusH==0L) 60000 else focusH
            var IF:Double = hourBP.IF

            val tvhMin: TextView = binding.hMIn
            val tvnPU: TextView = binding.nPU
            val tvFocus: TextView = binding.Focus
            val tvIF: TextView = binding.IF


            withContext(Dispatchers.Main) {
                binding.apply {
                    tvhMin.text = hMin.toString()
                    tvnPU.text = nPU.toString()
                    tvFocus.text = Focus.toString()
                    tvIF.text = IF.toString()
                }
            }
        }


        return binding.root
    }




}
