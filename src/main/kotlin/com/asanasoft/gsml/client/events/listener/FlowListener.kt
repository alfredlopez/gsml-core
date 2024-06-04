package com.asanasoft.gsml.client.events.listener

import kotlinx.coroutines.flow.Flow

interface FlowListener<T> : EventListener {
    var flowListener : Flow<T>?
}