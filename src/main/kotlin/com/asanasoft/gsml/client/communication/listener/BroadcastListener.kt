package com.asanasoft.gsml.client.communication.listener

import com.asanasoft.gsml.client.communication.Visitor

interface BroadcastListener : Listener, Visitor {
    var busy : Boolean
    fun start()
    fun stop()
}