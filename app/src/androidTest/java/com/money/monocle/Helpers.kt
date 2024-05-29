package com.money.monocle

import android.content.Context
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk


val userId = "id"
val username = "Evgen"

fun getString(@StringRes id: Int): String =
    ApplicationProvider.getApplicationContext<Context>().getString(id)

inline fun <reified T> mockTask(result: T? = null, exception: Exception? = null): Task<T> {
    val task = mockk<Task<T>>()
    every { task.isSuccessful } returns true
    every { task.result } returns result
    every { task.exception } returns exception
    every { task.isCanceled } returns false
    return task
}