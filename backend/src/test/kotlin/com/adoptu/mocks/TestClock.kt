package com.adoptu.mocks

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class TestClock(private var fixedTime: Instant = Instant.parse("2024-01-15T10:00:00Z")) : Clock {
    override fun now(): Instant = fixedTime

    fun setTime(instant: Instant) {
        fixedTime = instant
    }

    fun setTimeMillis(millis: Long) {
        fixedTime = Instant.fromEpochMilliseconds(millis)
    }

    fun advanceMillis(millis: Long) {
        fixedTime = Instant.fromEpochMilliseconds(fixedTime.toEpochMilliseconds() + millis)
    }
}
