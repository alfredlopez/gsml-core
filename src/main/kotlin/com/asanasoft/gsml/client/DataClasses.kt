package com.asanasoft.gsml.client

import com.asanasoft.gsml.client.token.AccessToken
import com.asanasoft.gsml.exception.TokenManagerException
import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable

@Serializable
data class ApigeeToken(
    val token : String,
    val access_token : String,
    val issued_at : String,
    val expires_in : String,
    val scope : String
)

@Serializable
data class RevokeBody(val period_time : Long)

@Serializable
data class Keys(val keys : ArrayList<Key>)

@Serializable
data class Key(val kid : String, val publicKey : String, val privateKey : String)

@Serializable
data class RevokedTokens(val status_requested : String, val refresh_tokens : ArrayList<String>)

@Serializable
data class ApigeeRequestBody(
    val client_id : String,
    val client_secret : String,
    val grant_type : String,
    val scope : String
)

@Serializable
data class RevokedToken(val refresh_token : String, val app_name : String?)

@Serializable
data class IdentityTokenRequest(
    val legacy_system_id : String,
    val session_timeout : String,
    val refresh_timeout : String,
    val requested_token_type : String,
    val legacy_keys : Map<String, String>
)

@Serializable
data class InitialIdentityToken(
    val token : String,
    val token_type : String,
    val expires_in : String,
    val refresh_token : String,
    val refresh_token_jti : String
)

@Serializable
data class AccessTokenRequest(
    val identity_token : String,
    val destination_id : String,
    val lang : String,
    val context_keys : Map<String, String>,
    val refresh_token : String,
    val requested_token_type : String
)

@Serializable
data class AccessTokenResponse(val token : String, val token_type : String, val expires_in : String)

data class Result<out T>(
    val value : T? = null,
    val cause : Throwable? = null,
    val isSuccess : Boolean = (value != null),
    val isFailure : Boolean = (cause != null)
)

data class LiveToken(val accessToken : AccessToken?, val job : Job)
data class ErrorData(val cause : TokenManagerException, val errorName : String, val message : String?, val payload : MutableMap<String, Any> = mutableMapOf())

fun noop() = Unit
fun noop(someValue : Any?) = someValue as Unit
val pass : Unit = Unit