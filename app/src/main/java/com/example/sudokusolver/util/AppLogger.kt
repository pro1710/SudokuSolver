package com.example.sudokusolver.util

import android.util.Log

interface AppLogger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warning(tag: String, message: String)
}

object AndroidAppLogger : AppLogger {
    override fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun warning(tag: String, message: String) {
        Log.w(tag, message)
    }
}
