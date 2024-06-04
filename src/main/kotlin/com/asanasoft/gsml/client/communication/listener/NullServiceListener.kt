package com.asanasoft.gsml.client.communication.listener

import com.asanasoft.gsml.client.noop

class NullServiceListener : AbstractPollingBroadcastListener() {
    override fun start() {
        if (next != null) next!!.start()
    }

    override fun stop() {
        if (next != null) next!!.stop()
    }

    override fun listen(context : Map<String, Any>?) {
        TODO("Not yet implemented")
    }

    override fun poll() {
        noop()
    }
}
