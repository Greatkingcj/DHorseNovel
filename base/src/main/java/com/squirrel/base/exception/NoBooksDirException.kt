package com.squirrel.base.exception

import com.squirrel.base.R
import splitties.init.appCtx

class NoBooksDirException: NoStackTraceException(appCtx.getString(R.string.no_books_dir))