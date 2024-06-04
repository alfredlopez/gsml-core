package com.asanasoft.gsml.client.events

data class Revoke(override val message : String) : AbstractEvent() {
    override val type : EventType = EventType.REVOKE
}
