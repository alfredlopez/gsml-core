package com.asanasoft.gsml.client.communication.broadcast

import com.asanasoft.gsml.client.configure.Configurator

class EventBroadcasterConfigurator : Configurator<EventBroadcaster> {
    override var configurationFile : String = "eventBroadcaster.properties"
}