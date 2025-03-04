package com.example.organizeme.Reduction

import kotlin.math.max
import kotlin.math.min

object DF_Algorithm {
    private const val MIN_DF: Double = 0.001
    private const val MAX_DF: Double = 0.05
//    private var DF:Double = 0.01

    fun calcDF(DF: Double, ST: Long, DL: Long, lastAA: Int): Double {
        val branch = chooseBranch(ST, DL, lastAA)
        if (branch == -1) {  //Decreasing branch
            return  max(MIN_DF , (DF/2))
        }
        else if (branch == 1) { // Increasing branch
            return  min(MAX_DF, (DF+0.01))
        }
        else {  //Neutral branch
            return DF
        }
    }

    private fun chooseBranch(ST: Long, DL: Long, lastAA: Int): Int {
        val x = diffST_DL(ST, DL) + AA(lastAA)
        if (x < -1) return -1
        else if (x > 1) return 1
        else return 0
    }

    private fun diffST_DL(ST: Long, DL: Long): Int {
        if (ST > DL) return -10
        else {
            val diffST_DL = DL - ST
            if (diffST_DL > (0.5 * DL).toLong()) {
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