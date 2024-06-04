package com.asanasoft.gsml.client.token

interface AccessToken : Token {
    override val type : TokenType.ACCESS
    var identityToken : IdentityToken
    var refreshToken : RefreshToken
    var refreshTokenId : String

    var identityClaimKey : String
    var refreshClaimKey : String
    var refreshIdClaimKey : String
    var destination : String
    var language : String

    fun getContext() : Map<String, String>?
}