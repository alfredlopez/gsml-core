package com.asanasoft.gsml.client.token.jwt.impl

import com.asanasoft.gsml.client.init.Environment
import com.asanasoft.gsml.client.token.IdentityToken
import com.asanasoft.gsml.client.token.TokenType

open class JwtIdentityToken : JweToken(), IdentityToken {
    override val type = TokenType.IDENTITY

    init {
        var properties = Environment.getProperties("identityToken.properties", true)

        principalClaim = properties?.getProperty("principalClaim").toString()
        excludedClaims = properties?.getProperty("excludedClaims").toString()
    }
}