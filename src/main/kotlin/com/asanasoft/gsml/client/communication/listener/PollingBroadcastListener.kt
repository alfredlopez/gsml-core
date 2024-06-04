package com.asanasoft.gsml.client.communication.listener

interface PollingBroadcastListener : BroadcastListener {
    var pollingInterval : Long
    fun poll()
}