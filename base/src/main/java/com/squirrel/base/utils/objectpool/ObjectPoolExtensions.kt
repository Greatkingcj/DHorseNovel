package com.squirrel.base.utils.objectpool

fun <T> ObjectPool<T>.synchronized(): ObjectPool<T> = ObjectPoolLocked(this)
