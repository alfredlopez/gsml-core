package com.asanasoft.gsml.client.token.jwt.impl

import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.SignedJWT
import mu.KotlinLogging
import java.security.interfaces.RSAPublicKey

private val logger = KotlinLogging.logger {}

open class JwsToken : JwtToken() {
    override fun validate() : Boolean {
        var result = super.validate()

        if (result) {
            with(parsedToken as SignedJWT) {
                result = validate(this.header.keyID)
            }
        }

        return result
    }

    override fun validate(keyId : String) : Boolean {
        logger.info("Validating $type token...")
        var result = false
        try {
            with(parsedToken as SignedJWT) {
                if (this.state == JWSObject.State.SIGNED) {
                    logger.debug("JwsToken token.state is Signed")
                    val publicKey : RSAPublicKey = JwkProviderServer.get(keyId).additionalAttributes.get("origPubKey") as RSAPublicKey
                    result = this.verify(RSASSAVerifier(publicKey))
                }
                else if (this.state == JWSObject.State.VERIFIED) {
                    logger.debug("JwsToken token.state is verified")
                    result = true
                }
                else {
                    logger.info("JwsToken token.state is not Signed and not Verified")
                }
            }
        }
        catch (e : Exception) {
            logger.error {
                "An error occurred verifying token: ${e.message}"
            }
        }
        logger.info("Validating result: $result")
        return result
    }
}