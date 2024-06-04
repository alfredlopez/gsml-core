package com.asanasoft.gsml.client.communication.listener

import com.asanasoft.gsml.client.ApigeeToken
import com.asanasoft.gsml.client.GSMLConstants
import com.asanasoft.gsml.client.Keys
import com.asanasoft.gsml.client.events.Refresh
import com.asanasoft.gsml.client.events.listener.EventListener
import com.asanasoft.gsml.client.events.listener.FlowListener
import com.asanasoft.gsml.client.noop
import com.asanasoft.gsml.client.token.jwt.JwkProvider
import com.asanasoft.gsml.client.utility.getClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.slf4j.MDC

private val logger = KotlinLogging.logger {}

