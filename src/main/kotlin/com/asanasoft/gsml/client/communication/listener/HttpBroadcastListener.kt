package com.asanasoft.gsml.client.communication.listener

interface HttpBroadcastListener : BroadcastListener {
    var pollingUrl : String
}