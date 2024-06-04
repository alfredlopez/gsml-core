package com.asanasoft.gsml.client.events

data class Register(override val message : String, val tokenManager : com.asanasoft.gsml.client.TokenManager) : AbstractEvent() {
    override val type : EventType = EventType.REGISTER

    init {
        this.payload?.put("tokenManager", tokenManager)
    }
}
