package com.manulife.gsml.test

import com.asanasoft.gsml.client.events.Event
import com.asanasoft.gsml.client.events.EventType.*
import com.asanasoft.gsml.client.events.Register
import com.asanasoft.gsml.client.events.listener.EventListener
import com.asanasoft.gsml.test.TestingTokenManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.util.*


private val logger = KotlinLogging.logger{}

class TokenManagerListener2 : EventListener {
    val ID = UUID.randomUUID()
    /**
     * Event triggered
     * Called by TokenManager when an event is received from the managing server
     *
     * @param event
     */
    override fun eventTriggered(event: Event?) {
        var tokenManager : TestingTokenManager?
        tokenManager = event?.get("tokenManager") as TestingTokenManager

        logger.info("Entering eventTriggered...from ${tokenManager.name}")

        runBlocking {
            delay(3000) //pretend we're doing something intense and "important" :-)
        }

        when (event?.type) {
            ERROR -> {
                logger.debug("An error occurred: ${event.message}")
                tokenManager.invalidate()
            }

            REVOKE -> {
                logger.debug("revoked")
            }
            REGISTER -> {
                val registerEvent : Register? = event.takeIf { it is Register } as Register
                val registeringTokenManager = registerEvent?.tokenManager as TestingTokenManager

                logger.debug("Registering with TokenManager: ${registeringTokenManager?.name}")
            }
            else -> {
                logger.debug("All quiet")
            }
        }

        logger.info("Done with eventTriggered")
    }
}