package com.asanasoft.gsml.client.token.jwt.impl

import com.asanasoft.gsml.client.init.Environment
import com.asanasoft.gsml.client.token.*
import com.asanasoft.gsml.client.utility.Injector
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

abstract class JwtAccessToken : JweToken(), AccessToken {
    override val type = TokenType.ACCESS

    override lateinit var identityToken : IdentityToken
    override lateinit var refreshToken : RefreshToken
    override lateinit var refreshTokenId : String

    final override var identityClaimKey     : String = ""
    final override var refreshClaimKey      : String = ""
    final override var refreshIdClaimKey    : String = ""

    override var destination : String = ""
    override var language : String = ""

    init {
        val properties = Environment.getProperties("accessToken.properties", true)

        identityClaimKey    = properties?.getProperty("identityClaimKey")       ?: ""
        refreshClaimKey     = properties?.getProperty("refreshTokenClaimKey")   ?: ""
        refreshIdClaimKey   = properties?.getProperty("refreshTokenIdClaimKey") ?: ""
        excludedClaims      = properties?.getProperty("excludedClaims")         ?: ""
    }

    override fun unmarshal() : Token? {
        var result : Token? = null

        try {
            super.unmarshal()
            val marshalledIdentityToken : String = claims?.get(identityClaimKey) ?: ""
            val marshalledRefreshToken : String = claims?.get(refreshClaimKey) ?: ""

            identityToken = Injector.inject(IdentityToken::class.java)
            refreshToken = Injector.inject(RefreshToken::class.java)

            identityToken.marshalled = marshalledIdentityToken
            refreshToken.marshalled = marshalledRefreshToken

            refreshTokenId = claims?.get(refreshIdClaimKey) ?: "" //This should ***ALWAYS*** exist!!!

            logger.debug("AccessToken payload: \n $payload")
            result = this
        }
        catch (e : Exception) {
            logger.error("An error occurred unmarshalling AccessToken:", e)
        }

        return result
    }

}