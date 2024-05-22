package com.algorigo.laxthaandchestbeltcollectapp.util.rxresult.data

import android.content.Intent

data class ActivityRequest(
    val intent: Intent,
    val isGranted: (() -> Boolean)? = null,
)
