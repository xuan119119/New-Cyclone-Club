package com.bba.new_cyclone_club.mvvm.logger

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 统一日志框架。
 *
 * ### 初始化（在 Application.onCreate() 中调用）
 * ```kotlin
 * Logger.init(
 *     config = LoggerConfig(
 *         minLevel   = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.WARN,
 *         globalTag  = "CycloneClub",
 *         writeToFile = BuildConfig.DEBUG,
 *         logDir      = getExternalFilesDir("logs")
 *     )
 * )
 * ```
 *
 * ### 使用
 * ```kotlin
 * Logger.d("点击了登录按钮")
 * Logger.e("网络错误", throwable)
 * Logger.i(tag = "Network", msg = "请求成功")
 * ```
 */
object Logger {

    private var config = LoggerConfig()
    private val fileExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // -------------------------------------------------------------------------
    // 初始化
    // -------------------------------------------------------------------------

    /**
     * 初始化日志配置，建议在 Application.onCreate() 中调用。
     * 不调用则使用默认配置（DEBUG 级别输出，不写文件）。
     */
    fun init(config: LoggerConfig) {
        this.config = config
    }

    // -------------------------------------------------------------------------
    // 日志方法
    // -------------------------------------------------------------------------

    fun v(msg: String, tag: String? = null) = log(LogLevel.VERBOSE, tag, msg)
    fun d(msg: String, tag: String? = null, throwable: Throwable? = null) = log(LogLevel.DEBUG, tag, msg, throwable)
    fun i(msg: String, tag: String? = null, throwable: Throwable? = null) = log(LogLevel.INFO, tag, msg, throwable)
    fun w(msg: String, tag: String? = null, throwable: Throwable? = null) = log(LogLevel.WARN, tag, msg, throwable)
    fun e(msg: String, tag: String? = null, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, msg, throwable)
    fun wtf(msg: String, tag: String? = null, throwable: Throwable? = null) = log(LogLevel.ASSERT, tag, msg, throwable)

    // -------------------------------------------------------------------------
    // 核心输出
    // -------------------------------------------------------------------------

    private fun log(level: LogLevel, tag: String?, msg: String, throwable: Throwable? = null) {
        if (level.priority < config.minLevel.priority) return

        val resolvedTag = tag ?: config.globalTag
        val fullMsg = buildMessage(msg, throwable)

        // Logcat 输出
        if (config.enableLogcat) {
            printLogcat(level, resolvedTag, fullMsg)
        }

        // 文件写入
        if (config.writeToFile && config.logDir != null) {
            writeToFile(level, resolvedTag, fullMsg)
        }

        // 自定义拦截器
        config.interceptors.forEach { it.onLog(level, resolvedTag, msg, throwable) }
    }

    private fun buildMessage(msg: String, throwable: Throwable?): String {
        if (throwable == null) return msg
        return "$msg\n${Log.getStackTraceString(throwable)}"
    }

    private fun printLogcat(level: LogLevel, tag: String, msg: String) {
        // Logcat 单条最大 4000 字符，超长则分段输出
        val maxLen = 3800
        if (msg.length <= maxLen) {
            logcatPrint(level, tag, msg)
        } else {
            msg.chunked(maxLen).forEachIndexed { idx, chunk ->
                logcatPrint(level, tag, "[$idx] $chunk")
            }
        }
    }

    private fun logcatPrint(level: LogLevel, tag: String, msg: String) {
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, msg)
            LogLevel.DEBUG   -> Log.d(tag, msg)
            LogLevel.INFO    -> Log.i(tag, msg)
            LogLevel.WARN    -> Log.w(tag, msg)
            LogLevel.ERROR   -> Log.e(tag, msg)
            LogLevel.ASSERT  -> Log.wtf(tag, msg)
        }
    }

    private fun writeToFile(level: LogLevel, tag: String, msg: String) {
        val dir = config.logDir ?: return
        fileExecutor.execute {
            try {
                if (!dir.exists()) dir.mkdirs()
                val fileName = "log_${fileDateFormat.format(Date())}.txt"
                val file = File(dir, fileName)
                val line = "${dateFormat.format(Date())} ${level.label}/$tag: $msg\n"
                PrintWriter(FileWriter(file, true)).use { it.print(line) }
            } catch (e: Exception) {
                Log.e("Logger", "写入日志文件失败", e)
            }
        }
    }
}

// =============================================================================
// 配置类
// =============================================================================

/**
 * Logger 配置。
 *
 * @param minLevel      最低输出级别（低于此级别的日志会被过滤）
 * @param globalTag     全局默认 Tag
 * @param enableLogcat  是否输出到 Logcat（默认 true）
 * @param writeToFile   是否写入文件（默认 false）
 * @param logDir        日志文件目录（writeToFile=true 时必填）
 * @param interceptors  自定义日志拦截器列表
 */
data class LoggerConfig(
    val minLevel: LogLevel = LogLevel.VERBOSE,
    val globalTag: String = "CycloneClub",
    val enableLogcat: Boolean = true,
    val writeToFile: Boolean = false,
    val logDir: File? = null,
    val interceptors: List<LogInterceptor> = emptyList()
)

// =============================================================================
// 日志级别
// =============================================================================

enum class LogLevel(val priority: Int, val label: String) {
    VERBOSE(2, "V"),
    DEBUG(3,   "D"),
    INFO(4,    "I"),
    WARN(5,    "W"),
    ERROR(6,   "E"),
    ASSERT(7,  "A")
}

// =============================================================================
// 拦截器接口（可接入 Firebase Crashlytics 等）
// =============================================================================

/**
 * 日志拦截器，可用于接入第三方崩溃上报平台。
 *
 * ```kotlin
 * class CrashlyticsInterceptor : LogInterceptor {
 *     override fun onLog(level: LogLevel, tag: String, msg: String, throwable: Throwable?) {
 *         if (level >= LogLevel.ERROR) {
 *             FirebaseCrashlytics.getInstance().recordException(throwable ?: RuntimeException(msg))
 *         }
 *     }
 * }
 * ```
 */
interface LogInterceptor {
    fun onLog(level: LogLevel, tag: String, msg: String, throwable: Throwable?)
}
