package com.lagradost.cloudstream3.utils

import android.app.Activity
import androidx.appcompat.app.AlertDialog

object SingleSelectionHelper {
    fun Activity.showBottomDialog(
        items: List<String>,
        selectedIndex: Int,
        title: String,
        isDismissible: Boolean,
        onDismiss: () -> Unit,
        onSelect: (Int) -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(items.toTypedArray()) { _, which ->
                onSelect(which)
            }
            .setOnDismissListener { onDismiss() }
            .show()
    }
}
