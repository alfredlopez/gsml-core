package com.asanasoft.gsml.client.token

sealed class TokenType {
    object JWT : TokenType()
    object JWS : TokenType()
    object JWE : TokenType()
    object ACCESS : TokenType()
    object IDENTITY : TokenType()
    object REFRESH : TokenType()
}

open class ExtendedTokenType : TokenType()
