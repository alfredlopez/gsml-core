package com.asanasoft.gsml.client.communication.broadcast

import com.asanasoft.gsml.client.events.Event
import com.asanasoft.gsml.client.Result
import com.asanasoft.gsml.client.noop

class NullServiceBroadcaster : AbstractEventBroadcaster() {
    override fun broadcast(event : Event?) {
        noop()
    }

    override fun broadcast(event : Event?, handler : ((Result<String>) -> Unit)?) {
        val result : Result<String> = Result()
        handler?.invoke(result)
    }
}
