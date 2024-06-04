package com.asanasoft.gsml.client.communication.broadcast

import com.asanasoft.gsml.client.ApigeeToken
import com.asanasoft.gsml.client.RevokedToken
import com.asanasoft.gsml.client.events.Event
import com.asanasoft.gsml.client.events.EventType.REVOKE
import com.asanasoft.gsml.client.init.Environment
import com.asanasoft.gsml.client.noop
import com.asanasoft.gsml.client.utility.getClient
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

