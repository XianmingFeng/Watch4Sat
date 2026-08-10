package com.xianming.watch4sat

import android.content.Intent

data class ExternalLaunchEnvelope(
    val eventId: Long,
    val intent: Intent
)
