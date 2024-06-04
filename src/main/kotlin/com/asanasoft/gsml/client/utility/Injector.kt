@file:Suppress("UNCHECKED_CAST")

package com.asanasoft.gsml.client.utility

import com.asanasoft.gsml.client.init.PropertiesLoader
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

object Injector {
    var properties : Properties
    var propertiesLoader : PropertiesLoader

    init {
        propertiesLoader = object : PropertiesLoader() {
            init {
                this.multiValues = true
                init()
            }
        }

        properties = propertiesLoader.createProperties("modules.properties")
        propertiesLoader.load("modules.properties", properties)
        properties.toString()
    }

    fun <T> inject(clazz : Class<out T>) : T {
        val result : T?

        if (properties.get(clazz.simpleName) is ArrayList<*>) {
            val classes = properties.get(clazz.simpleName) as ArrayList<String>

            var instance : T
            var previous : T? = null
            var top : T? = null

            for (className in classes) {
                instance = Class.forName(className).getConstructor().newInstance() as T

                if (top == null) {
                    top = instance
                    previous = top
                }

                if ((previous is Chainable<*>) && (instance is Chainable<*>)) {
                    (previous as Chainable<T>).next = instance
                    (instance as Chainable<T>).previous = previous

                    previous = instance
                }
            }

            result = top
        }
        else {
            val className = properties.getProperty(clazz.simpleName)
            result = Class.forName(className).getConstructor().newInstance() as T
        }

        return result!!
    }
}