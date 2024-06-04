package com.asanasoft.gsml.client.hooks.errors.impl

import com.asanasoft.gsml.client.ErrorData
import com.asanasoft.gsml.client.hooks.Hook
import com.asanasoft.gsml.client.hooks.errors.ErrorObject

import mu.KotlinLogging

private val logger = KotlinLogging.logger{}

open class DefaultErrorObject : ErrorObject {
    override var previous : Hook? = null
    override var next : Hook? = null

    override fun handle(context : ErrorData) {
        logger.debug("Handling: $context")
        next?.handle(context)
    }
}