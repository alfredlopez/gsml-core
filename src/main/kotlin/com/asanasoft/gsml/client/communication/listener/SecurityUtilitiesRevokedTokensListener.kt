package com.asanasoft.gsml.client.communication.listener

import com.asanasoft.gsml.client.*
import com.asanasoft.gsml.client.GSMLConstants.KEY_REVOKED_TOKEN
import com.asanasoft.gsml.client.events.Revoke
import com.asanasoft.gsml.client.events.listener.EventListener
import com.asanasoft.gsml.client.utility.getClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import org.slf4j.MDC

private val logger = KotlinLogging.logger {}

