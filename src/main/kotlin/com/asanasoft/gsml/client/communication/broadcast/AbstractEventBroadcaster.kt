@file:Suppress("LeakingThis", "LeakingThis")

package com.asanasoft.gsml.client.communication.broadcast

import com.asanasoft.gsml.client.events.Event
import com.asanasoft.gsml.client.Result
import com.asanasoft.gsml.client.noop

abstract class AbstractEventBroadcaster : EventBroadcaster {
    protected val configurator = EventBroadcasterConfigurator()

    init {
        configurator.configure(this)
    }

    override fun broadcast(event : Event?) {
        broadcast(event, null)
    }

    override fun broadcast(event : Event?, handler : ((Result<String>) -> Unit)?) {
        preBroadcast(event)
        doBroadcast(event)
        postBroadcast(event)
    }

    protected open fun preBroadcast(event : Event?) {
        noop()
    }

    protected open fun postBroadcast(event : Event?) {
        noop()
    }

    protected open fun doBroadcast(event : Event?, handler : ((Result<String>) -> Unit)? = null) {
        noop()
    }
}