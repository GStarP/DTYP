package com.example.dtyp.action

import android.content.Context
import android.content.Intent
import com.example.dtyp.action.accessibility.DTYPAccessibilityService
import com.example.dtyp.action.accessibility.DTYPActionType

fun doAction(context: Context, type: DTYPActionType) {
    val intent = Intent(DTYPAccessibilityService.INTENT_FILTER_NAME).apply {
        putExtra(DTYPAccessibilityService.INTENT_KEY_ACTION, type.name)
    }
    context.sendBroadcast(intent)
}
