package com.xianming.watch4sat

class ExternalLaunchEventSequence(
    startingEventId: Long = 0L
) {
    private var nextEventId = startingEventId.coerceAtLeast(0L)
    private var latestEventId: Long? = null

    val lastPublishedEventId: Long
        get() = nextEventId

    fun publish(): Long {
        nextEventId += 1L
        latestEventId = nextEventId
        return nextEventId
    }

    fun consumeIfLatest(eventId: Long): Boolean {
        if (latestEventId != eventId) return false
        latestEventId = null
        return true
    }
}
