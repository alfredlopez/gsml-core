package com.asanasoft.gsml.client.init

import org.slf4j.MDC

object Environment : PropertiesLoader() {
    private var initialized : Boolean = false

    init {
        init()
        MDC.put("app-name", getProperty("gsml_app"))
    }

    override fun getProperty(fromProperties : String, usingKey : String) : String? {
        if (systemProperties.isEmpty) init()

        return super.getProperty(fromProperties, usingKey)
    }
}