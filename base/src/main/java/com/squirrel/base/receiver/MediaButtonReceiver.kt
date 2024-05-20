package com.squirrel.base.Receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import com.squirrel.base.constant.EventBus
import com.squirrel.base.data.appDb
import com.squirrel.base.help.LifecycleHelp
import com.squirrel.base.help.config.AppConfig
import com.squirrel.base.model.AudioPlay
import com.squirrel.base.model.ReadAloud
import com.squirrel.base.model.ReadBook
import com.squirrel.base.service.AudioPlayService
import com.squirrel.base.service.BaseReadAloudService
import com.squirrel.base.utils.getPrefBoolean
import com.squirrel.base.utils.postEvent


/**
 * Created by GKF on 2018/1/6.
 * 监听耳机键
 */
class MediaButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (handleIntent(context, intent) && isOrderedBroadcast) {
            abortBroadcast()
        }
    }

    companion object {

        fun handleIntent(context: Context, intent: Intent): Boolean {
            val intentAction = intent.action
            if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
                @Suppress("DEPRECATION")
                val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    ?: return false
                val keycode: Int = keyEvent.keyCode
                val action: Int = keyEvent.action
                if (action == KeyEvent.ACTION_DOWN) {
                    when (keycode) {
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            if (context.getPrefBoolean("mediaButtonPerNext", false)) {
                                com.squirrel.base.model.ReadBook.moveToPrevChapter(true)
                            } else {
                                ReadAloud.prevParagraph(context)
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            if (context.getPrefBoolean("mediaButtonPerNext", false)) {
                                com.squirrel.base.model.ReadBook.moveToNextChapter(true)
                            } else {
                                ReadAloud.nextParagraph(context)
                            }
                        }
                        else -> readAloud(context)
                    }
                }
            }
            return true
        }

        fun readAloud(context: Context, isMediaKey: Boolean = true) {
            when {
                BaseReadAloudService.isRun -> {
                    if (BaseReadAloudService.isPlay()) {
                        ReadAloud.pause(context)
                        AudioPlay.pause(context)
                    } else {
                        ReadAloud.resume(context)
                        AudioPlay.resume(context)
                    }
                }
                AudioPlayService.isRun -> {
                    if (AudioPlayService.pause) {
                        AudioPlay.resume(context)
                    } else {
                        AudioPlay.pause(context)
                    }
                }
               /* LifecycleHelp.isExistActivity(ReadBookActivity::class.java) ->
                    postEvent(EventBus.MEDIA_BUTTON, true)
                LifecycleHelp.isExistActivity(com.squirrel.base.ui.audio.AudioPlayActivity::class.java) ->
                    postEvent(EventBus.MEDIA_BUTTON, true)*/
                else -> if (AppConfig.mediaButtonOnExit || LifecycleHelp.activitySize() > 0 || !isMediaKey) {
                    ReadAloud.upReadAloudClass()
                    if (com.squirrel.base.model.ReadBook.book != null) {
                        com.squirrel.base.model.ReadBook.readAloud()
                    } else {
                        appDb.bookDao.lastReadBook?.let {
                            com.squirrel.base.model.ReadBook.resetData(it)
                            com.squirrel.base.model.ReadBook.clearTextChapter()
                            com.squirrel.base.model.ReadBook.loadContent(false) {
                                com.squirrel.base.model.ReadBook.readAloud()
                            }
                        }
                    }
                }
            }
        }
    }

}
