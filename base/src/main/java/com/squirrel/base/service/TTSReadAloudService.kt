package com.squirrel.base.service

import android.app.PendingIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.squirrel.base.R
import com.squirrel.base.constant.AppConst
import com.squirrel.base.constant.AppLog
import com.squirrel.base.constant.AppPattern
import com.squirrel.base.exception.NoStackTraceException
import com.squirrel.base.help.MediaHelp
import com.squirrel.base.help.config.AppConfig
import com.squirrel.base.help.coroutine.Coroutine
import com.squirrel.base.lib.dialogs.SelectItem
import com.squirrel.base.model.ReadAloud
import com.squirrel.base.model.ReadBook
import com.squirrel.base.utils.GSON
import com.squirrel.base.utils.LogUtils
import com.squirrel.base.utils.fromJsonObject
import com.squirrel.base.utils.servicePendingIntent
import com.squirrel.base.utils.toastOnUi
import kotlinx.coroutines.ensureActive

/**
 * 本地朗读
 */
class TTSReadAloudService : BaseReadAloudService(), TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var ttsInitFinish = false
    private val ttsUtteranceListener = TTSUtteranceListener()
    private var speakJob: Coroutine<*>? = null
    private val TAG = "TTSReadAloudService"

    override fun onCreate() {
        super.onCreate()
        initTts()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearTTS()
    }

    @Synchronized
    private fun initTts() {
        ttsInitFinish = false
        val engine = GSON.fromJsonObject<SelectItem<String>>(ReadAloud.ttsEngine).getOrNull()?.value
        LogUtils.d(TAG, "initTts engine:$engine")
        textToSpeech = if (engine.isNullOrBlank()) {
            TextToSpeech(this, this)
        } else {
            TextToSpeech(this, this, engine)
        }
        upSpeechRate()
    }

    @Synchronized
    fun clearTTS() {
        textToSpeech?.runCatching {
            stop()
            shutdown()
        }
        textToSpeech = null
        ttsInitFinish = false
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let {
                it.setOnUtteranceProgressListener(ttsUtteranceListener)
                ttsInitFinish = true
                play()
            }
        } else {
            toastOnUi(R.string.tts_init_failed)
        }
    }

    @Synchronized
    override fun play() {
        if (!ttsInitFinish) return
        if (!requestFocus()) return
        if (contentList.isEmpty()) {
            AppLog.putDebug("朗读列表为空")
            com.squirrel.base.model.ReadBook.readAloud()
            return
        }
        super.play()
        MediaHelp.playSilentSound(this@TTSReadAloudService)
        speakJob?.cancel()
        speakJob = execute {
            LogUtils.d(TAG, "朗读列表大小 ${contentList.size}")
            LogUtils.d(TAG, "朗读页数 ${textChapter?.pageSize}")
            val tts = textToSpeech ?: throw NoStackTraceException("tts is null")
            var result = tts.runCatching {
                speak("", TextToSpeech.QUEUE_FLUSH, null, null)
            }.getOrElse {
                AppLog.put("tts出错\n${it.localizedMessage}", it, true)
                TextToSpeech.ERROR
            }
            if (result == TextToSpeech.ERROR) {
                AppLog.put("tts出错 尝试重新初始化")
                clearTTS()
                initTts()
                return@execute
            }
            val contentList = contentList
            for (i in nowSpeak until contentList.size) {
                ensureActive()
                var text = contentList[i]
                if (paragraphStartPos > 0 && i == nowSpeak) {
                    text = text.substring(paragraphStartPos)
                }
                if (text.matches(AppPattern.notReadAloudRegex)) {
                    continue
                }
                result = tts.runCatching {
                    speak(text, TextToSpeech.QUEUE_ADD, null, AppConst.APP_TAG + i)
                }.getOrElse {
                    AppLog.put("tts出错\n${it.localizedMessage}", it, true)
                    TextToSpeech.ERROR
                }
                if (result == TextToSpeech.ERROR) {
                    AppLog.put("tts朗读出错:$text")
                }
            }
            LogUtils.d(TAG, "朗读内容添加完成")
        }.onError {
            AppLog.put("tts朗读出错\n${it.localizedMessage}", it, true)
        }
    }

    override fun playStop() {
        textToSpeech?.runCatching {
            stop()
        }
    }

    /**
     * 更新朗读速度
     */
    override fun upSpeechRate(reset: Boolean) {
        if (AppConfig.ttsFlowSys) {
            if (reset) {
                clearTTS()
                initTts()
            }
        } else {
            val speechRate = (AppConfig.ttsSpeechRate + 5) / 10f
            textToSpeech?.setSpeechRate(speechRate)
        }
    }

    /**
     * 暂停朗读
     */
    override fun pauseReadAloud(abandonFocus: Boolean) {
        super.pauseReadAloud(abandonFocus)
        speakJob?.cancel()
        textToSpeech?.runCatching {
            stop()
        }
    }

    /**
     * 恢复朗读
     */
    override fun resumeReadAloud() {
        super.resumeReadAloud()
        play()
    }

    /**
     * 朗读监听
     */
    private inner class TTSUtteranceListener : UtteranceProgressListener() {

        private val TAG = "TTSUtteranceListener"

        override fun onStart(s: String) {
            LogUtils.d(TAG, "onStart nowSpeak:$nowSpeak pageIndex:$pageIndex utteranceId:$s")
            textChapter?.let {
                if (readAloudNumber + 1 > it.getReadLength(pageIndex + 1)) {
                    pageIndex++
                    com.squirrel.base.model.ReadBook.moveToNextPage()
                }
                upTtsProgress(readAloudNumber + 1)
            }
        }

        override fun onDone(s: String) {
            LogUtils.d(TAG, "onDone utteranceId:$s")
            //跳过全标点段落
            do {
                readAloudNumber += contentList[nowSpeak].length + 1 - paragraphStartPos
                paragraphStartPos = 0
                nowSpeak++
                if (nowSpeak >= contentList.size) {
                    nextChapter()
                    return
                }
            } while (contentList[nowSpeak].matches(AppPattern.notReadAloudRegex))
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            super.onRangeStart(utteranceId, start, end, frame)
            val msg =
                "$TAG onRangeStart nowSpeak:$nowSpeak pageIndex:$pageIndex utteranceId:$utteranceId start:$start end:$end frame:$frame"
            LogUtils.d(TAG, msg)
            textChapter?.let {
                if (readAloudNumber + start > it.getReadLength(pageIndex + 1)) {
                    pageIndex++
                    com.squirrel.base.model.ReadBook.moveToNextPage()
                    upTtsProgress(readAloudNumber + start)
                }
            }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            LogUtils.d(
                TAG,
                "onError nowSpeak:$nowSpeak pageIndex:$pageIndex utteranceId:$utteranceId errorCode:$errorCode"
            )
        }

        @Deprecated("Deprecated in Java")
        override fun onError(s: String) {
            //nothing
        }

    }

    override fun aloudServicePendingIntent(actionStr: String): PendingIntent? {
        return servicePendingIntent<TTSReadAloudService>(actionStr)
    }

}