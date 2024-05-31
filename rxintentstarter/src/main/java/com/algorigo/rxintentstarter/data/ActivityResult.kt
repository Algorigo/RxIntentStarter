package com.algorigo.rxintentstarter.data

import android.content.Intent

data class ActivityResult(
    val resultCode: Int,
    val data: Intent?
) {

    fun isOk(): Boolean {
        return resultCode == android.app.Activity.RESULT_OK
    }

    fun isCanceled(): Boolean {
        return resultCode == android.app.Activity.RESULT_CANCELED
    }
}
