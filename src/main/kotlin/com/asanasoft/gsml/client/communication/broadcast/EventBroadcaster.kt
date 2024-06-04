package com.asanasoft.gsml.client.communication.broadcast

import com.asanasoft.gsml.client.events.Event
import com.asanasoft.gsml.client.Result

interface EventBroadcaster {
    fun broadcast(event : Event?)
    fun broadcast(event : Event?, handler : ((Result<String>) -> Unit)? = null)
}