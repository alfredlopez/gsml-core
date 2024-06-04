package com.asanasoft.gsml.client

import com.asanasoft.gsml.client.events.Event
import com.asanasoft.gsml.client.events.listener.EventListener
import com.asanasoft.gsml.client.utility.Injector

/**
 * TokenManagerProxy is a helper class for Spring Boot to use TokenManager as a bean...
 */
open class TokenManagerProxy {
    private var delegate : com.asanasoft.gsml.client.TokenManager

    init {
        delegate = Injector.inject(com.asanasoft.gsml.client.TokenManager::class.java)
    }

    open var accessToken : String?
        get() = delegate.accessToken
        set(value) {
            delegate.accessToken = value
        }
    open var isValid : Boolean
        get() = delegate.isValid
        set(value) {
            noop(value)
        }
    open var principal : String?
        get() = delegate.principal
        set(value) {
            noop(value)
        }
    open var context : Map<String, String>?
        get() = delegate.context
        set(value) {
            noop(value)
        }
    open var id : String?
        get() = delegate.id
        set(value) {
            noop(value)
        }
    open var multiTokenMode : Boolean
        get() = delegate.multiTokenMode
        set(value) {
            delegate.multiTokenMode = value
        }

    open fun createIdentityToken(
        sourceSystem : String,
        sessionTimeout : Int,
        refreshTimeout : Int,
        principalId : String,
        additionalData : MutableMap<String, String>
    ) {
        delegate.createIdentityToken(sourceSystem, sessionTimeout, refreshTimeout, principalId, additionalData)
    }

    open fun createIdentityToken(
        sourceSystem : String,
        sessionTimeout : Int,
        refreshTimeout : Int,
        principalId : String,
        additionalData : MutableMap<String, String>,
        handler : ((Result<String>) -> Unit)?
    ) {
        delegate.createIdentityToken(sourceSystem, sessionTimeout, refreshTimeout, principalId, additionalData, false, handler)
    }

    open fun createIdentityToken(principal : String) {
        delegate.createIdentityToken(principal)
    }

    open fun createIdentityToken(principal : String, handler : ((Result<String>) -> Unit)?) {
        delegate.createIdentityToken(principal, handler)
    }

    open fun createAccessToken(destinationId : String, language : String, context : Map<String, String>, encrypted : Boolean = false) : String? {
        return delegate.createAccessToken(destinationId, language, context, encrypted)
    }

    open fun createAccessToken(
        destinationId : String,
        language : String,
        context : Map<String, String>,
        handler : ((Result<String>) -> Unit)? = null
    ) : String? {
        return delegate.createAccessToken(destinationId, language, context, false, handler)
    }

    open fun createAccessToken(
        destinationId : String,
        language : String,
        context : Map<String, String>,
        encrypted : Boolean = false,
        handler : ((Result<String>) -> Unit)?
    ) : String? {
        return delegate.createAccessToken(destinationId, language, context, encrypted, handler)
    }

    open fun register(listener : EventListener) {
        delegate.register(listener)
    }

    open fun unregister(listener : EventListener) {
        delegate.unregister(listener)
    }

    open fun notify(event : Event) {
        delegate.notify(event)
    }

    open fun invalidate() {
        delegate.invalidate()
    }

    open fun getContextValue(key : String) : String? {
        return delegate.getContextValue(key)
    }

    open fun eventTriggered(event : Event?) {
        delegate.eventTriggered(event)
    }
}