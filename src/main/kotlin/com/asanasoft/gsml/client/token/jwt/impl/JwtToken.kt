package com.asanasoft.gsml.client.token.jwt.impl

import com.asanasoft.gsml.client.noop
import com.asanasoft.gsml.client.token.AbstractToken
import com.asanasoft.gsml.client.token.Token
import com.asanasoft.gsml.client.token.TokenType
import com.asanasoft.gsml.client.utility.jsonFormat
import com.asanasoft.gsml.client.utility.jsonToMap
import com.asanasoft.gsml.exception.TokenManagerException
import com.nimbusds.jose.proc.SimpleSecurityContext
import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

open class JwtToken : AbstractToken() {
    var claims : MutableMap<String, String>? = mutableMapOf()

    var excludedClaims : String = ""
        protected set

    var jti : String = ""
        private set

    protected val verifier = DefaultJWTClaimsVerifier<SimpleSecurityContext>(null, null)
    protected var principalClaim : String = ""
    protected val jwkProvider = JwkProviderServer
    protected lateinit var parsedToken : JWT

    protected var subject : String
        get() {
            return parsedToken.jwtClaimsSet.subject
        }
        set(value) {
            noop(value)
        }

    override fun unmarshal() : Token? {
        var result : Token? = null

        try {
            logger.debug("Starting JWT.decode...")
            parsedToken = JWTParser.parse(marshalled)
            logger.debug("Finished JWT.decode...")

            //Essentially, call validate AND set "isValid"
            if (isValid) {
                jsonToMap(jsonFormat.parseToJsonElement(parsedToken.jwtClaimsSet.toString(true)), claims!!)
                result = this
            }
        }
        catch (e : Exception) {
            logger.error("An error occurred in unmarshal(): ${e.message}")
        }

        return result
    }

    override fun validate() : Boolean {
        var result = false

        try {
            verifier.maxClockSkew = 0
            verifier.verify(parsedToken.jwtClaimsSet, null)
            result = true
        }
        catch (e : Exception) {
            logger.error(e) {
                "An error occurred validating JWT: ${e.message}"
            }

            /**
             * Create an exception, but don't throw it.
             * This is to give the configured ErrorObject an opportunity
             * to react to the exception
             */
            TokenManagerException("An error occurred validating JWT").initCause(e)
        }
        return result
    }

    override val type : TokenType = TokenType.JWT

    override var issuedAt : Date
        get() {
            return parsedToken.jwtClaimsSet.issueTime
        }
        set(value) {
            noop(value)
        }

    override var expiresAt : Date
        get() {
            return parsedToken.jwtClaimsSet.expirationTime
        }
        set(value) {
            noop(value)
        }

    override var principal : String = ""
        get() {
            if (field.isEmpty()) {
                field = parsedToken.jwtClaimsSet.claims[principalClaim]?.toString() ?: ""
            }
            return field
        }
        set(value) {
            noop(value)
        }

    override var nonce : String = ""
    override var payload : String = ""
        get() {
            if (field.isBlank()) {
                field = parsedToken.jwtClaimsSet.toPayload().toString()
            }
            return field
        }
        set(value) {
            noop(value)
        }

    override fun populateContext(context : MutableMap<String, String>) {
        if (this.payload.isNotBlank()) {
            val payloadJson = Json.parseToJsonElement(this.payload)
            jsonToMap(payloadJson, context, this.excludedClaims)
        }
    }
}