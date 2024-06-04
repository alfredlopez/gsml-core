package com.asanasoft.gsml.client.communication.broadcast

import com.asanasoft.gsml.client.Result
import com.asanasoft.gsml.client.events.Event
import com.asanasoft.gsml.client.utility.Injector

object EventBroadcasterHub : EventBroadcaster {
    private val delegate : EventBroadcaster = Injector.inject(EventBroadcaster::class.java)
    override fun broadcast(event : Event?) {
        delegate.broadcast(event)
    }

    override fun broadcast(event: Event?, handler: ((Result<String>) -> Unit)?) {
        delegate.broadcast(event, handler)
    }
}