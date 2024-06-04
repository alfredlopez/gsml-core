package com.asanasoft.gsml.client.events

data class TokenError(override val message : String?) : AbstractEvent() {
    override val type = EventType.ERROR
}