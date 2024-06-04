package com.asanasoft.gsml.client.events

data class Wake(override val message : String) : AbstractEvent() {
    override val type : EventType = EventType.WAKE
}
