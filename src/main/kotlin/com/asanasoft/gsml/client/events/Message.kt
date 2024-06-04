package com.asanasoft.gsml.client.events

data class Message(override val message : String) : AbstractEvent() {
    override val type = EventType.EVENT
}
