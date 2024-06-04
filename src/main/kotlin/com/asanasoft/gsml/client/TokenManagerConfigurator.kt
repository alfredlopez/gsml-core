package com.asanasoft.gsml.client

import com.asanasoft.gsml.client.configure.Configurator

class TokenManagerConfigurator : Configurator<com.asanasoft.gsml.client.TokenManager> {
    override var configurationFile: String = "tokenManager.properties"
}