package com.asanasoft.gsml.client.configure

import com.asanasoft.gsml.client.init.Environment
import mu.KotlinLogging
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties

/**
 * Configurator
 *
 * @param
 * @constructor Create empty Configurator
 */
private val logger = KotlinLogging.logger {}

interface Configurator<T> {
    var configurationFile : String

    /**
     * Configure
     *
     * @param configurable
     */
    fun configure(configurable : T) {
        logger.debug("Configuring " + configurable!!::class.simpleName)

        //See if the config filename is set in the application's properties file, else, use default...
        val configFilename = Environment.getProperty(configurationFile) ?: configurationFile
        var properties = Environment.getProperties(configFilename, true)

        val myClass = configurable.javaClass.kotlin
        val memberProperties = myClass.memberProperties

        for (property in memberProperties) {
            if (property is KMutableProperty<*>) {
                if (property.returnType == String::class.createType()) {
                    property.setter.call(configurable, properties?.getProperty(property.name))
                }
                if (property.returnType == Int::class.createType()) {
                    logger.debug("Setting Integer " + property.name)
                    property.setter.call(configurable, properties?.getProperty(property.name)?.toInt())
                }
                if (property.returnType == Long::class.createType()) {
                    logger.debug("Setting Long " + property.name)
                    property.setter.call(configurable, properties?.getProperty(property.name)?.toLong())
                }
                if (property.returnType == Double::class.createType()) {
                    logger.debug("Setting Double " + property.name)
                    property.setter.call(configurable, properties?.getProperty(property.name)?.toDouble())
                }
            }
        }
    }
}
