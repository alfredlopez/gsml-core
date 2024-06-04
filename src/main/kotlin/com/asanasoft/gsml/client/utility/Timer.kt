package com.asanasoft.gsml.client.utility

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

fun CoroutineScope.launchPeriodicAsync(
    repeatMillis : Long,
    action : () -> Unit
) = this.launchPeriodicAsync(repeatMillis, false, action = action)

fun CoroutineScope.launchPeriodicAsync(
    repeatMillis : Long,
    delayFirst : Boolean,
    id : String = UUID.randomUUID().toString(),
    action : () -> Unit
) = this.launch {
    logger.trace("Starting a timer with id = $id...")
    if (repeatMillis > 0) {
        while (isActive) {
            if (delayFirst) delay(repeatMillis)
            logger.trace("Firing timer id = $id...")
            action()
            if (!delayFirst) delay(repeatMillis)
        }
    }
    else {
        logger.trace("Firing one-time timer id = $id...")
        action()
    }
}