package com.lagradost.cloudstream3.ui.settings

object Globals {
    const val PHONE = 1
    const val TV = 2
    const val EMULATOR = 4

    fun isLayout(flags: Int): Boolean {
        return (flags and PHONE) != 0
    }
}
