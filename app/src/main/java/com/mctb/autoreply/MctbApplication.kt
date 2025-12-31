package com.mctb.autoreply

import android.app.Application
import timber.log.Timber

/**
 * Application class for MCTB Auto-Reply.
 * Initializes logging and any other app-wide services.
 */
class MctbApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            // In debug builds, log everything to Logcat
            Timber.plant(Timber.DebugTree())
        } else {
            // In release builds, use a production-ready tree
            Timber.plant(ProductionTree())
        }

        Timber.i("MCTB Auto-Reply application started (v${BuildConfig.VERSION_NAME})")
    }

    /**
     * Production logging tree that filters log levels and handles crashes gracefully.
     */
    private class ProductionTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Only log warnings and errors in production
            if (priority >= android.util.Log.WARN) {
                // Log to system log
                android.util.Log.println(priority, tag ?: "MCTB", message)

                // If there's a throwable, log stack trace
                t?.let {
                    android.util.Log.println(priority, tag ?: "MCTB", it.stackTraceToString())
                }

                // TODO: In a real production app, you would send errors to a crash reporting
                // service like Firebase Crashlytics here
                // Example: FirebaseCrashlytics.getInstance().recordException(t)
            }
        }
    }
}
