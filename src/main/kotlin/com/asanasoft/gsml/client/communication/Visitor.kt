package com.asanasoft.gsml.client.communication

import com.asanasoft.gsml.client.events.Event
import com.asanasoft.gsml.client.events.listener.EventListener

interface Visitor {
    fun register(listener : EventListener)
    fun unregister(listener : EventListener)
    fun notify(event : Event)
    fun flush()
}