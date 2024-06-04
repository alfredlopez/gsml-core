package com.asanasoft.gsml.client.events

/**
 * This represents an TokenManager event.
 *
 * @constructor Create empty Event
 */
interface Event {
    val message : String?
    val payload : MutableMap<String, Any?>?
    val type : EventType
    fun get(key : String) : Any?
    fun put(key : String, value : Any?)
}