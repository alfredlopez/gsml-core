package com.asanasoft.gsml.client.communication.listener

import com.asanasoft.gsml.client.configure.Configurator

class BroadcastListenerConfigurator : Configurator<BroadcastListener> {
    override var configurationFile : String = "broadcastListener.properties"
}