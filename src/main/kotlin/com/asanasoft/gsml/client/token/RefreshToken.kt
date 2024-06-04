package com.asanasoft.gsml.client.token

interface RefreshToken : Token {
    override val type : TokenType.REFRESH
}