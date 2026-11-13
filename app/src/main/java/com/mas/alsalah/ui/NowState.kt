package com.mas.alsalah.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** ساعة تتحدث كل ثانية فقط أثناء ظهور الشاشة (STARTED)، وتتوقف تلقائيًا في الخلفية. */
@Composable
fun rememberNowState(): MutableLongState {
    val st = remember { mutableLongStateOf(System.currentTimeMillis()) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val n = System.currentTimeMillis()
                st.longValue = n
                delay(1000 - (n % 1000) + 2)
            }
        }
    }
    return st
}
