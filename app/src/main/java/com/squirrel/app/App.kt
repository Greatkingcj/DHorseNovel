package com.squirrel.app

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import com.github.liuyueyi.quick.transfer.constants.TransType
import com.jeremyliao.liveeventbus.LiveEventBus
import com.jeremyliao.liveeventbus.logger.DefaultLogger
import com.squirrel.app.BuildConfig
import com.squirrel.app.R
import com.squirrel.base.base.AppContextWrapper
import com.squirrel.base.constant.AppConst.channelIdDownload
import com.squirrel.base.constant.AppConst.channelIdReadAloud
import com.squirrel.base.constant.AppConst.channelIdWeb
import com.squirrel.base.constant.PreferKey
import com.squirrel.base.data.appDb
import com.squirrel.base.help.AppWebDav
import com.squirrel.base.help.CrashHandler
import com.squirrel.base.help.DefaultData
import com.squirrel.base.help.LifecycleHelp
import com.squirrel.base.help.RuleBigDataHelp
import com.squirrel.base.help.book.BookHelp
import com.squirrel.base.help.config.AppConfig
import com.squirrel.base.help.config.ThemeConfig.applyDayNight
import com.squirrel.base.help.coroutine.Coroutine
import com.squirrel.base.help.http.Cronet
import com.squirrel.base.help.http.ObsoleteUrlFactory
import com.squirrel.base.help.http.okHttpClient
import com.squirrel.base.help.source.SourceHelp
import com.squirrel.base.help.storage.Backup
import com.squirrel.base.model.BookCover
import com.squirrel.base.utils.ChineseUtils
import com.squirrel.base.utils.LogUtils
import com.squirrel.base.utils.defaultSharedPreferences
import com.squirrel.base.utils.getPrefBoolean
import kotlinx.coroutines.launch
import splitties.init.appCtx
import splitties.systemservices.notificationManager
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class App : Application() {

    private lateinit var oldConfig: Configuration

    override fun onCreate() {
        super.onCreate()
        LogUtils.d("App", "onCreate")
        LogUtils.logDeviceInfo()
        oldConfig = Configuration(resources.configuration)
        CrashHandler(this)
        //预下载Cronet so
        Cronet.preDownload()
        createNotificationChannels()
        LiveEventBus.config()
            .lifecycleObserverAlwaysActive(true)
            .autoClear(false)
            .enableLogger(BuildConfig.DEBUG || AppConfig.recordLog)
            .setLogger(EventLogger())
        applyDayNight(this)
        registerActivityLifecycleCallbacks(LifecycleHelp)
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(AppConfig)
        DefaultData.upVersion()
        Coroutine.async {
            URL.setURLStreamHandlerFactory(ObsoleteUrlFactory(okHttpClient))
            launch { installGmsTlsProvider(appCtx) }
            //初始化封面
            BookCover.toString()
            //清除过期数据
            appDb.cacheDao.clearDeadline(System.currentTimeMillis())
            if (getPrefBoolean(PreferKey.autoClearExpired, true)) {
                val clearTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
                appDb.searchBookDao.clearExpired(clearTime)
            }
            RuleBigDataHelp.clearInvalid()
            BookHelp.clearInvalidCache()
            Backup.clearCache()
            //初始化简繁转换引擎
            when (AppConfig.chineseConverterType) {
                1 -> launch {
                    ChineseUtils.fixT2sDict()
                }

                2 -> ChineseUtils.preLoad(true, TransType.SIMPLE_TO_TRADITIONAL)
            }
            //调整排序序号
            SourceHelp.adjustSortNumber()
            //同步阅读记录
            if (AppConfig.syncBookProgress) {
                AppWebDav.downloadAllBookProgress()
            }
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppContextWrapper.wrap(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val diff = newConfig.diff(oldConfig)
        if ((diff and ActivityInfo.CONFIG_UI_MODE) != 0) {
            applyDayNight(this)
        }
        oldConfig = Configuration(newConfig)
    }

    /**
     * 尝试在安装了GMS的设备上(GMS或者MicroG)使用GMS内置的Conscrypt
     * 作为首选JCE提供程序，而使Okhttp在低版本Android上
     * 能够启用TLSv1.3
     * https://f-droid.org/zh_Hans/2020/05/29/android-updates-and-tls-connections.html
     * https://developer.android.google.cn/reference/javax/net/ssl/SSLSocket
     *
     * @param context
     * @return
     */
    private fun installGmsTlsProvider(context: Context) {
        try {
            val gms = context.createPackageContext(
                "com.google.android.gms",
                CONTEXT_INCLUDE_CODE or CONTEXT_IGNORE_SECURITY
            )
            gms.classLoader
                .loadClass("com.google.android.gms.common.security.ProviderInstallerImpl")
                .getMethod("insertProvider", Context::class.java)
                .invoke(null, gms)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 创建通知ID
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val downloadChannel = NotificationChannel(
            channelIdDownload,
            getString(R.string.action_download),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val readAloudChannel = NotificationChannel(
            channelIdReadAloud,
            getString(R.string.read_aloud),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val webChannel = NotificationChannel(
            channelIdWeb,
            getString(R.string.web_service),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        //向notification manager 提交channel
        notificationManager.createNotificationChannels(
            listOf(
                downloadChannel,
                readAloudChannel,
                webChannel
            )
        )
    }

    class EventLogger : DefaultLogger() {

        override fun log(level: Level, msg: String) {
            super.log(level, msg)
            LogUtils.d(TAG, msg)
        }

        override fun log(level: Level, msg: String, th: Throwable?) {
            super.log(level, msg, th)
            LogUtils.d(TAG, "$msg\n${th?.stackTraceToString()}")
        }

        companion object {
            private const val TAG = "[LiveEventBus]"
        }
    }

}
