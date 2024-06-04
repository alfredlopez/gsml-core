package com.asanasoft.gsml.server

import com.asanasoft.gsml.client.IdentityTokenRequest
import com.asanasoft.gsml.client.InitialIdentityToken

interface TokenIssuer {
    fun getIdentityToken(principal : IdentityTokenRequest) : InitialIdentityToken?
    fun getAccessToken(
        identityToken : String,
        destinationId : String,
        language : String,
        context : Map<String, String>,
        refreshToken : String,
        tokenType : String
    ) : String?
}