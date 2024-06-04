package com.asanasoft.gsml.client.communication.listener

import com.asanasoft.gsml.client.events.Event
import com.asanasoft.gsml.client.events.listener.EventListener
import com.asanasoft.gsml.client.init.Environment
import com.asanasoft.gsml.client.utility.Injector
import mu.KotlinLogging
import org.slf4j.MDC

private val logger = KotlinLogging.logger {}

/**
 * Broadcast listener hub
 * This is a singleton that will service all instances of TokenManager in an
 * application. This will reduce the number of calls made from an application
 * that implements this client library.
 */
object BroadcastListenerHub : BroadcastListener {
    private val delegate = Injector.inject(BroadcastListener::class.java)

    init {
        MDC.put("gsml-app", Environment.getProperty("gsml_app"))
        MDC.put("object-id", this.toString())
        logger.info("Starting service...")
        start()
        MDC.remove("object-id")
    }

    override var busy : Boolean = false
        get() = delegate.busy

    override fun register(listener : EventListener) {
        delegate.register(listener)
    }

    override fun unregister(listener : EventListener) {
        delegate.unregister(listener)
    }

    override fun notify(event : Event) {
        delegate.notify(event)
    }

    override fun flush() {
        delegate.flush()
    }

    override fun start() {
        delegate.start()
    }

    override fun stop() {
        delegate.stop()
    }

    override fun listen(context : Map<String, Any>?) {
        delegate.listen(context)
    }
}