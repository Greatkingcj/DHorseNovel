package com.squirrel.base.base

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squirrel.base.help.coroutine.Coroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import splitties.init.appCtx
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
open class BaseViewModel(application: Application) : AndroidViewModel(application) {

    val context: Context by lazy { this.getApplication() }

    fun <T> execute(
        scope: CoroutineScope = viewModelScope,
        context: CoroutineContext = Dispatchers.IO,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        executeContext: CoroutineContext = Dispatchers.Main,
        block: suspend CoroutineScope.() -> T
    ): Coroutine<T> {
        return Coroutine.async(scope, context, start, executeContext, block)
    }

    fun <R> submit(
        scope: CoroutineScope = viewModelScope,
        context: CoroutineContext = Dispatchers.IO,
        block: suspend CoroutineScope.() -> Deferred<R>
    ): Coroutine<R> {
        return Coroutine.async(scope, context) { block().await() }
    }

}