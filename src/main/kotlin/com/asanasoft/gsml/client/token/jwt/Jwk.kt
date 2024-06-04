package com.asanasoft.gsml.client.token.jwt

import com.asanasoft.gsml.client.token.InvalidPublicKeyException
import java.security.PublicKey

interface Jwk {
    val id : String?
    val type : String?
    val algorithm : String?
    val usage : String?
    val operationsAsList : List<String>?
    val certificateUrl : String?
    val certificateChain : List<String>?
    val certificateThumbprint : String?
    val additionalAttributes : Map<String, Any>

    @get:Throws(InvalidPublicKeyException::class)
    val publicKey : PublicKey
}