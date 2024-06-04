package com.asanasoft.gsml.client.events

data class Flush(override val message : String) : AbstractEvent() {
    override val type: EventType = EventType.UNREGISTER
}
