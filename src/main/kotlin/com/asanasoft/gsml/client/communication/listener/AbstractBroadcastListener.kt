package com.asanasoft.gsml.client.communication.listener

import com.asanasoft.gsml.client.events.Event
import com.asanasoft.gsml.client.events.Flush
import com.asanasoft.gsml.client.events.listener.EventListener
import com.asanasoft.gsml.client.utility.Chainable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

abstract class AbstractBroadcastListener : BroadcastListener, Chainable<BroadcastListener> {
    protected val configurator = BroadcastListenerConfigurator()
    protected var started = false
    protected var supervisedJob = SupervisorJob()
    protected val scope = CoroutineScope(Dispatchers.Default + supervisedJob)

    override var previous : BroadcastListener? = null
    override var next : BroadcastListener? = null

    var eventListeners = mutableListOf<EventListener>()
        private set

    override fun register(listener : EventListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)

            if (!started) start() //eventListeners.isNotEmpty implies started==true
        }

        this.next?.register(listener)
    }

    override fun unregister(listener : EventListener) {
        if (eventListeners.contains(listener)) {
            eventListeners.remove(listener)

            if (eventListeners.isEmpty()) stop()
        }

        this.next?.unregister(listener)
    }

    override fun notify(event : Event) {
        logger.debug("Notifying with event " + event::class.simpleName)

        val listeners : List<EventListener> = eventListeners.toList()
        listeners.forEach {
            scope.launch {
                preNotify(event)
                it.eventTriggered(event)
                postNotify(event)
            }
        }
    }

    override fun start() {
        started = true
        this.next?.start()
    }

    override fun stop() {
        started = false
        this.next?.stop()
    }

    override fun flush() {
        val other = next
        next = null //Temporarily disconnect from the chain since flushing this doesn't necessarily mean flushing all...

        val event = Flush("Unregistering from BroadcastListener")
        eventListeners.forEach {
            scope.launch {
                it.eventTriggered(event)
            }
        }

        eventListeners.clear()

        stop()

        next = other //restore
        next?.flush() //allow the chain to determine whether or not flushing is necessary...
    }

    protected fun preNotify(event : Event) {
        event.message
    }

    protected fun postNotify(event : Event) {
        event.message
    }
}