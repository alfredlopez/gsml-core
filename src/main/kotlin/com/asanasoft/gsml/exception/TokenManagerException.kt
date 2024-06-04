package com.asanasoft.gsml.exception

import com.asanasoft.gsml.client.ErrorData
import com.asanasoft.gsml.client.hooks.errors.impl.ErrorObjectHub

open class TokenManagerException(override val message : String, val reference : Any? = null) : Exception(message) {
    init {
        val context = ErrorData(this, this.javaClass.simpleName, this.message)

        reference?.let {
            context.payload.put("reference", reference)
        }

        ErrorObjectHub.handle(context)
    }
}