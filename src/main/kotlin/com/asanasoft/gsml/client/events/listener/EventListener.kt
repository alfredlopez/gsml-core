package com.asanasoft.gsml.client.events.listener

import com.asanasoft.gsml.client.events.Event

/**
 * Classes that need to listen to TokenManager events
 * should implement this interface and register with the
 * TokenManager instance via <code>TokenManager.register(EventListener)<code<
 *
 * @constructor Create empty Event listener
 */
interface EventListener {
    /**
     * Event triggered
     * Called by TokenManager when an event is received from the managing server
     *
     * @param event
     */
    fun eventTriggered(event : Event?)
}