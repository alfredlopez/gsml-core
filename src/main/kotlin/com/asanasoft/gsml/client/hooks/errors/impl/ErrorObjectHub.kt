package com.asanasoft.gsml.client.hooks.errors.impl

import com.asanasoft.gsml.client.ErrorData
import com.asanasoft.gsml.client.hooks.Hook
import com.asanasoft.gsml.client.hooks.errors.ErrorObject
import com.asanasoft.gsml.client.utility.Injector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object ErrorObjectHub : ErrorObject {
    private val delegate : ErrorObject = Injector.inject(ErrorObject::class.java)

    override fun handle(context : ErrorData) {
        runBlocking {
            CoroutineScope(Dispatchers.IO).launch {
                delegate.handle(context)
            }
        }
    }

    override var previous   : Hook? = null
    override var next       : Hook? = null
}