package com.asanasoft.gsml.client.communication.listener

interface Listener {
    fun listen(context : Map<String, Any>?)
}