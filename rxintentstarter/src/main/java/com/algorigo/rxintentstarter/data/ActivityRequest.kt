package com.algorigo.rxintentstarter.data

import android.content.Intent

data class ActivityRequest(
    val intent: Intent,
    val isGranted: (() -> Boolean)? = null,
)
