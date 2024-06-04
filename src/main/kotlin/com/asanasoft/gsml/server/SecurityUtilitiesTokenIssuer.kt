package com.asanasoft.gsml.server

import com.asanasoft.gsml.client.*
import com.asanasoft.gsml.client.init.Environment
import com.asanasoft.gsml.client.utility.getClient
import com.asanasoft.gsml.client.utility.jsonFormat
import com.asanasoft.gsml.exception.TokenManagerException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

