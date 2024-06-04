package com.asanasoft.gsml.client.events

data class NoToken(override val message : String) : AbstractEvent() {
    override val type = EventType.NO_TOKEN
}