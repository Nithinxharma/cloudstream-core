package com.lagradost.cloudstream3.utils

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment

fun Context.colorFromAttribute(attrRes: Int): Int = 0

object UIHelper {
    @JvmStatic
    fun navigate(fragment: Fragment, actionId: Int, args: Bundle? = null) {}
}
