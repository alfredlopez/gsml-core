package com.asanasoft.gsml.client.hooks

import com.asanasoft.gsml.client.ErrorData
import com.asanasoft.gsml.client.utility.Chainable

interface Hook : Chainable<Hook> {

    /**
     * Handle the error using the provided context
     */
    fun handle(context : ErrorData)
}