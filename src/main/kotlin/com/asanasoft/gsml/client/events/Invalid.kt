package com.asanasoft.gsml.client.events

data class Invalid(override val message : String) : AbstractEvent() {
    override val type = EventType.INVALID
}