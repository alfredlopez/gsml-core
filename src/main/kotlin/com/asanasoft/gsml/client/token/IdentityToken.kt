package com.asanasoft.gsml.client.token

interface IdentityToken : Token {
    override val type : TokenType.IDENTITY
}