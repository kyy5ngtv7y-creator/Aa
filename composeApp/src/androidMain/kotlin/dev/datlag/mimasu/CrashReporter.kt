package dev.datlag.mimasu

import android.content.Context

/**
 * Stores the stack trace of a fatal exception so the next launch can display
 * it. This build ships without Crashlytics reporting to a real project, so the
 * trace would otherwise be lost on a device we cannot attach a debugger to.
 */
object CrashReporter {

    private const val PREFS = "mimasu_crash_report"
    private const val KEY = "last_crash"

    fun install(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val report = buildString {
                    appendLine("Thread: ${thread.name}")
                    appendLine()
                    appendLine(error.stackTraceToString())
                }
                // commit(), not apply(): the process is about to die.
                prefs.edit().putString(KEY, report).commit()
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** Returns the stored trace once, then clears it. */
    fun consume(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val report = prefs.getString(KEY, null)
        if (report != null) {
            prefs.edit().remove(KEY).commit()
        }
        return report
    }
}
