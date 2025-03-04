package com.example.organizeme.UI

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.organizeme.DB.Dao
import com.example.organizeme.DB.Pickup_Database
import com.example.organizeme.R
import com.example.organizeme.BusinessLogic.ScreenTimeService
import com.example.organizeme.databinding.FragmentDayBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class DayFragment : Fragment(R.layout.fragment_day) {
    private var _binding: FragmentDayBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: Pickup_Database
    private lateinit var dao: Dao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDayBinding.inflate(inflater, container, false)

        // Initialize database and DAO here
        db = Pickup_Database.getDatabase(requireContext())
        dao = db.dao()


        val tvST: TextView = binding.ST
        val ST: LiveData<Long> = ScreenTimeService.ST
        ST.observe(viewLifecycleOwner, Observer {
            var seconds = (it / 1000).toInt()
            var minutes = seconds / 60
            var hours = minutes / 60
            minutes %= 60
            seconds = seconds % 60
            tvST.text =  String.format("%02d:%02d:%02d", hours, minutes, seconds)
        })

        lifecycleScope.launch(Dispatchers.IO) {
            delay(500)
            val day = dao.getDay(LocalDate.now().toString())
            val dayLimit : Long? = day?.DL
            val dPU = dao.getDayPU(day.dayID)
            val DF = day?.DF

            withContext(Dispatchers.Main) {
                binding.apply {
                    val tvDL: TextView = binding.DL
                    val tvDF: TextView = binding.DF
                    val tvdPU: TextView = binding.dPU

                    tvdPU.text = dPU?.toString() ?: "No data"
                    tvDL.text = dayLimit.toString() ?: "No data"
                    tvDF.text = DF?.toString() ?: "No data"
                }
            }
        }


        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
