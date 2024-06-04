package com.asanasoft.gsml.client.events

data class Refresh(override val message : String) : AbstractEvent() {
    override val type : EventType = EventType.REFRESH
}