package com.lagradost.cloudstream3

import android.app.Activity
import android.content.Context
import android.util.Log
import java.lang.ref.WeakReference

object CommonActivity {
    private var _activityRef: WeakReference<Activity>? = null
    var activity: Activity?
        get() = _activityRef?.get()
        set(value) { _activityRef = if (value == null) null else WeakReference(value) }

    fun showToast(msg: String?, length: Int = 0) {
        Log.i("CommonActivity", "Toast (headless): ${msg ?: ""}")
    }
    fun showToast(resId: Int, length: Int = 0) {
        Log.i("CommonActivity", "ToastRes (headless): $resId")
    }
}
