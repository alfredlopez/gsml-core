package com.asanasoft.gsml.client.token.jwt

import com.asanasoft.gsml.client.token.jwt.impl.SigningKeyNotFoundException

interface JwkProvider {
    @Throws(SigningKeyNotFoundException::class)
    fun get(keyId : String) : Jwk
}