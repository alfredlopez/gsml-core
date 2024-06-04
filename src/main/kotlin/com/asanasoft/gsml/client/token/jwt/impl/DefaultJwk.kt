package com.asanasoft.gsml.client.token.jwt.impl

import com.asanasoft.gsml.client.token.InvalidPublicKeyException
import com.asanasoft.gsml.client.token.jwt.Jwk
import org.apache.commons.codec.binary.Base64
import java.math.BigInteger
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.RSAPublicKeySpec

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
data class DefaultJwk(
    override val id : String?,
    override val type : String?,
    override val algorithm : String?,
    override val usage : String?,
    override val operationsAsList : List<String>?,
    override val certificateUrl : String?,
    override val certificateChain : List<String>?,
    override val certificateThumbprint : String?,
    override val additionalAttributes : Map<String, Any>
) : Jwk {
    @get:Throws(InvalidPublicKeyException::class)
    override val publicKey : PublicKey
        get() = if (!"RSA".equals(type, ignoreCase = true)) {
            throw InvalidPublicKeyException("The key is not of type RSA")
        }
        else {
            try {
                val kf = KeyFactory.getInstance("RSA")
                val modulus = BigInteger(1, Base64.decodeBase64(stringValue("n")))
                val exponent = BigInteger(1, Base64.decodeBase64(stringValue("e")))
                kf.generatePublic(RSAPublicKeySpec(modulus, exponent))
            }
            catch (var4 : InvalidKeySpecException) {
                throw InvalidPublicKeyException("Invalid public key")
            }
            catch (var5 : NoSuchAlgorithmException) {
                throw InvalidPublicKeyException("Invalid algorithm to generate key")
            }
        }

    protected fun stringValue(key : String) : String? {
        return additionalAttributes[key] as String?
    }
}