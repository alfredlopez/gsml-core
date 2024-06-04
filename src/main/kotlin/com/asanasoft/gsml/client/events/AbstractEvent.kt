package com.asanasoft.gsml.client.events

abstract class AbstractEvent : Event {
    override val message : String? = ""
    override val payload : MutableMap<String, Any?>? = mutableMapOf()

    override fun put(key : String, value : Any?) {
        this.payload?.put(key, value)
    }

    override fun get(key : String) : Any? {
        return payload?.get(key)
    }

    final override fun toString() : String {
        return "type: " + type.name + ", message: " + message + ", payload: " + payload.toString()
    }
}