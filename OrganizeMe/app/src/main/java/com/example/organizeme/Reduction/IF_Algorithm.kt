package com.example.organizeme.Reduction

import kotlin.math.max
import kotlin.math.min

object IF_Algorithm {
    private const val MIN_IF: Double = 0.001 //1 second if floored
    private const val MAX_IF: Double = 0.01 //15 seconds

    fun calcIF(IF: Double, focus: Long, HM: Long, lastAA: Int): Double {
        val branch = chooseBranch(focus, HM, lastAA)
        if (branch == -1) {  //Decreasing branch
            return  max(MIN_IF , (IF/5))
        }
        else if (branch == 1) { // Increasing branch
            return  min(MAX_IF, (IF+0.001))
        }
        else {  //Neutral branch
            return IF
        }
    }

    private fun chooseBranch(focus: Long, HM: Long, lastAA: Int): Int {
        val x = diffF_HM(focus, HM) + AA(lastAA)
        if (x < -1) return -1
        else if (x > 1) return 1
        else return 0
    }

    private fun diffF_HM(focus: Long, HM: Long): Int {
        if(focus < HM) return -10
        else{
            val diffF_HM = focus - HM
            if (diffF_HM >= HM) {
                return 2
            } else return 0
        }
    }

    private fun AA(lastAA: Int): Int {
        if (lastAA == 0) return -10
        else if (lastAA % 3 == 0) return lastAA / 3
        else return lastAA % 3 - 2
    }

}