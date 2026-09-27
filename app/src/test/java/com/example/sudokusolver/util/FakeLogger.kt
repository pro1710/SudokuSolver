package com.example.sudokusolver.util

class FakeLogger : AppLogger {
    data class Entry(val level: String, val tag: String, val message: String)

    val entries = mutableListOf<Entry>()

    override fun debug(tag: String, message: String) {
        entries.add(Entry("DEBUG", tag, message))
    }

    override fun info(tag: String, message: String) {
        entries.add(Entry("INFO", tag, message))
    }

    override fun warning(tag: String, message: String) {
        entries.add(Entry("WARNING", tag, message))
    }
}
