package com.asanasoft.gsml.client.communication.listener

import com.asanasoft.gsml.client.events.TokenError
import kotlinx.coroutines.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

abstract class AbstractPollingBroadcastListener : AbstractBroadcastListener(), PollingBroadcastListener {
    private val ONE_SECOND = 1000L
    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        logger.error("An error occurred polling: ${exception.message}")
        val errorEvent = TokenError(exception.message!!)
        notify(errorEvent)
    }
    private var polling : Boolean = false
        private set(newValue) {
            field = newValue
            busy = field
        }

    private var pollingJob : Job? = null

    override var pollingInterval : Long = 0

    override var busy : Boolean = false
        set(newValue : Boolean) {
            /**
             * We want to synchronize <code>busy</code> with <code>polling</code>
             */
            if (newValue.equals(polling)) field = newValue
        }

    protected fun doPoll() {
        polling = true
        poll()
        polling = false
    }

    override fun start() {
        if (!started) {
            pollingJob = scope.launch(errorHandler) {
                /**
                 * Originally a "while loop", but this is to try and take care a race condition.
                 * A do/while is unfavorable because it forces the system to take actiion at least once
                 * even though it should be allowed to...
                 */
                do {
                    if (!polling) doPoll()
                    delay(pollingInterval * ONE_SECOND)
                } while (this.isActive)
            }

            super.start() //call this for CoR...
        }
    }

    override fun stop() {
        logger.info("Stopping polling job...")

        if (pollingJob?.isActive == true) {
            pollingJob?.cancel()
            pollingJob = null
        }

        super.stop() //call this for CoR...
    }
}