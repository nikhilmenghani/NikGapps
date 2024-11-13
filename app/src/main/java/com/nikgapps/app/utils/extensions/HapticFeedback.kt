package com.nikgapps.app.utils.extensions

import android.view.HapticFeedbackConstants
import android.view.View

object HapticFeedback {
    fun View.slightHapticFeedback() =
        this.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

    fun View.longPressHapticFeedback() =
        this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}