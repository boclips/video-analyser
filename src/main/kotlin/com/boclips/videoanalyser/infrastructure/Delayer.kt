package com.boclips.videoanalyser.infrastructure

interface Delayer {
    fun delay(seconds: Int, onComplete: () -> Unit)
    fun delay(milliseconds: Long, onComplete: () -> Unit)
}
