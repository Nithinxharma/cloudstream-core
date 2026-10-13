package com.lagradost.cloudstream3.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import com.lagradost.cloudstream3.CloudStreamApp

object UIHelper {
    @ColorInt
    fun Context.colorFromAttribute(@AttrRes attr: Int): Int {
        return 0
    }

    fun Context.checkWrite(): Boolean = true

    fun Activity.requestRW() {}

    fun Activity.navigate(resId: Int, args: Bundle? = null) {}

    fun clipboardHelper(label: UiText, text: String) {
        val ctx = CloudStreamApp.context ?: return
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label.asStringNull(ctx), text)
        clipboard?.setPrimaryClip(clip)
    }
}
