package com.asanasoft.gsml.client.events

data class Success(override val message : String) : AbstractEvent() {
    override val type : EventType = EventType.SUCCESS
}
