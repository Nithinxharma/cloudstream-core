package com.lagradost.cloudstream3.ui

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.lagradost.cloudstream3.R

enum class WatchType(val internalId: Int, @StringRes val stringRes: Int) {
    NONE(0, R.string.none),
    WATCHING(1, R.string.type_watching),
    COMPLETED(2, R.string.type_completed),
    ONHOLD(3, R.string.type_on_hold),
    DROPPED(4, R.string.type_dropped),
    PLANTOWATCH(5, R.string.type_plan_to_watch),
    REWATCHING(6, R.string.type_re_watching);

    companion object {
        fun fromInternalId(id: Int?): WatchType = entries.find { it.internalId == id } ?: NONE
    }
}

enum class SyncWatchType(val internalId: Int, @StringRes val stringRes: Int) {
    NONE(0, R.string.none),
    WATCHING(1, R.string.type_watching),
    COMPLETED(2, R.string.type_completed),
    ONHOLD(3, R.string.type_on_hold),
    DROPPED(4, R.string.type_dropped),
    PLANTOWATCH(5, R.string.type_plan_to_watch),
    REWATCHING(6, R.string.type_re_watching);

    companion object {
        fun fromInternalId(id: Int?): SyncWatchType = entries.find { it.internalId == id } ?: NONE
    }
}

open class BaseFragment<V> : Fragment() {
    open var binding: V? = null
}

class WebviewFragment : BaseFragment<Any>() {
    companion object {
        fun newInstance(url: String): android.os.Bundle = android.os.Bundle().apply {
            putString("url", url)
        }
    }
}
