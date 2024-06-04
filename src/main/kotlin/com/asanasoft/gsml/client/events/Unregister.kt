package com.asanasoft.gsml.client.events

data class Unregister(override val message : String, val tokenManager : com.asanasoft.gsml.client.TokenManager) : AbstractEvent() {
    override val type : EventType = EventType.UNREGISTER
}
