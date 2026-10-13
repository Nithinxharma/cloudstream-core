package com.lagradost.cloudstream3.mvvm

import androidx.activity.ComponentActivity
import androidx.lifecycle.LiveData
import com.lagradost.cloudstream3.ui.BaseFragment

/** NOTE: Only one observer at a time per value */
fun <T> ComponentActivity.observe(liveData: LiveData<T>, action: (T) -> Unit) {
    observeNullable(liveData) { t -> t?.run(action) }
}

/** NOTE: Only one observer at a time per value */
fun <T> ComponentActivity.observeNullable(liveData: LiveData<T>, action: (T?) -> Unit) {
    liveData.removeObservers(this)
    liveData.observe(this, action)
}

/** NOTE: Only one observer at a time per value */
fun <T, V> BaseFragment<V>.observe(liveData: LiveData<T>, action: (T) -> Unit) {
    observeNullable(liveData) { t -> t?.run(action) }
}

fun <T, V> BaseFragment<V>.observeNullable(
    liveData: LiveData<T>, action: (T?) -> Unit
) {
    liveData.removeObservers(this)
    liveData.observe(this, action)
}