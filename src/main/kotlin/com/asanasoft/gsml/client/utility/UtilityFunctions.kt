package com.asanasoft.gsml.client.utility

import com.asanasoft.gsml.client.init.Environment
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.internal.toLongOrDefault
import org.apache.commons.codec.binary.Base64
import java.util.*
import kotlinx.serialization.json.Json as KotlinJson

fun decodeBase64ToString(encoded : String) : String {
    return String(Base64.decodeBase64(encoded), charset("UTF-8"))
}

fun jsonToMap(json : JsonElement, map : MutableMap<String, String>, excludedClaims : String = "") {
    if (json is JsonObject) {
        for (key in json.keys) {
            if (!excludedClaims.contains(key)) {
                if (json[key] is JsonPrimitive) {
                    map.put(key, (json[key] as JsonPrimitive).content)
                }
                else {
                    jsonToMap(json[key]!!, map, excludedClaims)
                }
            }
        }
    }

    if (json is JsonArray) {
        for (elem in json.iterator()) {
            jsonToMap(elem, map, excludedClaims)
        }
    }
}

val jsonFormat = KotlinJson {
    ignoreUnknownKeys = true
    isLenient = true
    allowStructuredMapKeys = true
}

/**
 * Turn on/off HttpClient logging...
 */
val serverProperties : Properties = Environment.getProperties("serverAccess.properties", true)!!
val httpClientLogging = serverProperties.getProperty("httpClientLogging").toBoolean()

fun getClient(serialize : Boolean = true) : HttpClient {
    val httpClient = HttpClient(OkHttp) {
        defaultRequest {
            serverProperties.forEach {
                val key = it.key.toString()

                if (key.startsWith("header.")) {
                    val headerKey = key.split(".")[1]
                    headers.appendIfNameAbsent(headerKey, it.value.toString())
                }
            }
        }

        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay()
        }

        if (httpClientLogging) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }

        //We will let KTor unmarshal the JSON into our data classes...
        if (serialize) {
            install(ContentNegotiation) {
                json(KotlinJson {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis =
                serverProperties.getProperty("requestTimeoutMillis").toLongOrDefault(30000) //how long the server has to respond to the request...
            connectTimeoutMillis =
                serverProperties.getProperty("connectTimeoutMillis").toLongOrDefault(30000) //how long the server has to allow the connection...
            socketTimeoutMillis =
                serverProperties.getProperty("socketTimeoutMillis").toLongOrDefault(30000) //how long the server has to respond between packets...
        }
    }

    return httpClient
}

/**
 * If you're wondering why this function even exists, it is because of Fortify.
 * Fortify doesn't take into consideration the fact that you can't always
 * whitelist AND you don't always know where these files (properties files, in this
 * case) are kept.
 */
fun cleanString(aString : String?) : String? {
    var cleanString : String? = null

    aString?.let {
        cleanString = ""
        for (element in it) {
            cleanString += cleanChar(element)
        }
    }

    return cleanString
}

private fun cleanChar(aChar : Char) : Char {
    // 0 - 9
    for (i in 48..57) {
        if (aChar.code == i) return i.toChar()
    }

    // 'A' - 'Z'
    for (i in 65..90) {
        if (aChar.code == i) return i.toChar()
    }

    // 'a' - 'z'
    for (i in 97..122) {
        if (aChar.code == i) return i.toChar()
    }
    when (aChar) {
        '/' -> return '/'
        '.' -> return '.'
        '-' -> return '-'
        '_' -> return '_'
        ' ' -> return ' '
    }
    return '%'
}
