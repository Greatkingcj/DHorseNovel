package com.squirrel.base.model

import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.squirrel.base.constant.EventBus
import com.squirrel.base.constant.IntentAction
import com.squirrel.base.constant.Status
import com.squirrel.base.data.appDb
import com.squirrel.base.data.entities.Book
import com.squirrel.base.data.entities.BookChapter
import com.squirrel.base.data.entities.BookSource
import com.squirrel.base.help.book.ContentProcessor
import com.squirrel.base.help.book.getBookSource
import com.squirrel.base.help.coroutine.Coroutine
import com.squirrel.base.service.AudioPlayService
import com.squirrel.base.utils.postEvent
import com.squirrel.base.utils.startService
import splitties.init.appCtx

@Suppress("unused")
object AudioPlay {
    var titleData = MutableLiveData<String>()
    var coverData = MutableLiveData<String>()
    var status = Status.STOP
    var book: Book? = null
    var durChapter: BookChapter? = null
    var inBookshelf = false
    var bookSource: BookSource? = null
    val loadingChapters = arrayListOf<Int>()
    var durChapterIndex = 0

    fun upData(context: Context, book: Book) {
        AudioPlay.book = book
        upDurChapter(book)
        if (durChapterIndex != book.durChapterIndex) {
            durChapterIndex = book.durChapterIndex
            playNew(context)
        }
    }

    fun resetData(context: Context, book: Book) {
        stop(context)
        AudioPlay.book = book
        titleData.postValue(book.name)
        coverData.postValue(book.getDisplayCover())
        bookSource = book.getBookSource()
        durChapterIndex = book.durChapterIndex
        upDurChapter(book)
    }

    /**
     * 播放当前章节
     */
    fun play(context: Context) {
        book?.let {
            if (durChapter == null) {
                upDurChapter(it)
            }
            durChapter?.let {
                context.startService<AudioPlayService> {
                    action = IntentAction.play
                }
            }
        }
    }

    /**
     * 从头播放新章节
     */
    fun playNew(context: Context) {
        book?.let {
            if (durChapter == null) {
                upDurChapter(it)
            }
            durChapter?.let {
                context.startService<AudioPlayService> {
                    action = IntentAction.playNew
                }
            }
        }
    }

    /**
     * 更新当前章节
     */
    fun upDurChapter(book: Book) {
        durChapter = appDb.bookChapterDao.getChapter(book.bookUrl, book.durChapterIndex)
        postEvent(EventBus.AUDIO_SUB_TITLE, durChapter?.title ?: "")
        postEvent(EventBus.AUDIO_SIZE, durChapter?.end?.toInt() ?: 0)
        postEvent(EventBus.AUDIO_PROGRESS, book.durChapterPos)
    }

    fun pause(context: Context) {
        if (AudioPlayService.isRun) {
            context.startService<AudioPlayService> {
                action = IntentAction.pause
            }
        }
    }

    fun resume(context: Context) {
        if (AudioPlayService.isRun) {
            context.startService<AudioPlayService> {
                action = IntentAction.resume
            }
        }
    }

    fun stop(context: Context) {
        if (AudioPlayService.isRun) {
            context.startService<AudioPlayService> {
                action = IntentAction.stop
            }
        }
    }

    fun adjustSpeed(context: Context, adjust: Float) {
        if (AudioPlayService.isRun) {
            context.startService<AudioPlayService> {
                action = IntentAction.adjustSpeed
                putExtra("adjust", adjust)
            }
        }
    }

    fun adjustProgress(context: Context, position: Int) {
        if (AudioPlayService.isRun) {
            context.startService<AudioPlayService> {
                action = IntentAction.adjustProgress
                putExtra("position", position)
            }
        }
    }

    fun skipTo(context: Context, index: Int) {
        Coroutine.async {
            book?.let { book ->
                book.durChapterIndex = index
                book.durChapterPos = 0
                durChapterIndex = book.durChapterIndex
                durChapter = null
                saveRead()
                playNew(context)
            }
        }
    }

    fun prev(context: Context) {
        Coroutine.async {
            book?.let { book ->
                if (book.durChapterIndex > 0) {
                    book.durChapterIndex -= 1
                    book.durChapterPos = 0
                    durChapterIndex = book.durChapterIndex
                    durChapter = null
                    saveRead()
                    play(context)
                } else {
                    stop(context)
                }
            }
        }
    }

    fun next(context: Context) {
        book?.let { book ->
            if (book.durChapterIndex + 1 < book.totalChapterNum) {
                book.durChapterIndex += 1
                book.durChapterPos = 0
                durChapterIndex = book.durChapterIndex
                durChapter = null
                saveRead()
                play(context)
            } else {
                stop(context)
            }
        }
    }

    fun setTimer(minute: Int) {
        if (AudioPlayService.isRun) {
            val intent = Intent(appCtx, AudioPlayService::class.java)
            intent.action = IntentAction.setTimer
            intent.putExtra("minute", minute)
            appCtx.startService(intent)
        } else {
            AudioPlayService.timeMinute = minute
            postEvent(EventBus.AUDIO_DS, minute)
        }
    }

    fun addTimer() {
        val intent = Intent(appCtx, AudioPlayService::class.java)
        intent.action = IntentAction.addTimer
        appCtx.startService(intent)
    }

    fun saveRead() {
        book?.let { book ->
            book.lastCheckCount = 0
            book.durChapterTime = System.currentTimeMillis()
            Coroutine.async {
                appDb.bookChapterDao.getChapter(book.bookUrl, book.durChapterIndex)?.let {
                    book.durChapterTitle = it.getDisplayTitle(
                        ContentProcessor.get(book.name, book.origin).getTitleReplaceRules(),
                        book.getUseReplaceRule()
                    )
                }
                book.update()
            }
        }
    }

    /**
     * 保存章节长度
     */
    fun saveDurChapter(audioSize: Long) {
        Coroutine.async {
            durChapter?.let {
                it.end = audioSize
                appDb.bookChapterDao.update(it)
            }
        }
    }
}