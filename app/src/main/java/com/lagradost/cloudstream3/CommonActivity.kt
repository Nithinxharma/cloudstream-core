package com.lagradost.cloudstream3

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.lagradost.cloudstream3.utils.Coroutines.runOnMainThread

object CommonActivity {
    private const val TAG = "CommonActivity"
    var activity: Activity? = null

    fun showToast(message: String?, duration: Int = Toast.LENGTH_SHORT) {
        if (message.isNullOrEmpty()) return
        Log.i(TAG, "Headless Toast: $message")
        runOnMainThread {
            val ctx = activity ?: CloudStreamApp.context
            ctx?.let {
                try {
                    Toast.makeText(it, message, duration).show()
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot show toast in headless mode", e)
                }
            }
        }
    }

    fun showToast(act: Activity?, message: String?, duration: Int = Toast.LENGTH_SHORT) {
        if (message.isNullOrEmpty()) return
        Log.i(TAG, "Headless Toast: $message")
        runOnMainThread {
            val ctx = act ?: activity ?: CloudStreamApp.context
            ctx?.let {
                try {
                    Toast.makeText(it, message, duration).show()
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot show toast in headless mode", e)
                }
            }
        }
    }

    fun showToast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        runOnMainThread {
            val ctx = activity ?: CloudStreamApp.context
            ctx?.let {
                try {
                    val msg = it.getString(resId)
                    Log.i(TAG, "Headless Toast ($resId): $msg")
                    Toast.makeText(it, resId, duration).show()
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot show toast in headless mode", e)
                }
            }
        }
    }

    fun showToast(act: Activity?, resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        runOnMainThread {
            val ctx = act ?: activity ?: CloudStreamApp.context
            ctx?.let {
                try {
                    val msg = it.getString(resId)
                    Log.i(TAG, "Headless Toast ($resId): $msg")
                    Toast.makeText(it, resId, duration).show()
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot show toast in headless mode", e)
                }
            }
        }
    }

    fun showToast(text: com.lagradost.cloudstream3.utils.UiText, duration: Int = Toast.LENGTH_SHORT) {
        val ctx = activity ?: CloudStreamApp.context
        showToast(text.asStringNull(ctx), duration)
    }

    fun showToast(act: Activity?, text: com.lagradost.cloudstream3.utils.UiText, duration: Int = Toast.LENGTH_SHORT) {
        val ctx = act ?: activity ?: CloudStreamApp.context
        showToast(act, text.asStringNull(ctx), duration)
    }
}
