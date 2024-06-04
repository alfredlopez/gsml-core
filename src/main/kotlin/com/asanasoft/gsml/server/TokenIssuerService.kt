package com.asanasoft.gsml.server

import com.asanasoft.gsml.client.IdentityTokenRequest
import com.asanasoft.gsml.client.InitialIdentityToken
import com.asanasoft.gsml.client.utility.Injector

object TokenIssuerService : TokenIssuer {
    private val delegate : TokenIssuer = Injector.inject(TokenIssuer::class.java)

    override fun getIdentityToken(principal : IdentityTokenRequest) : InitialIdentityToken? {
        return delegate.getIdentityToken(principal)
    }

    override fun getAccessToken(
        identityToken : String,
        destinationId : String,
        language : String,
        context : Map<String, String>,
        refreshToken : String,
        tokenType : String
    ) : String? {
        return delegate.getAccessToken(identityToken, destinationId, language, context, refreshToken, tokenType)
    }
}