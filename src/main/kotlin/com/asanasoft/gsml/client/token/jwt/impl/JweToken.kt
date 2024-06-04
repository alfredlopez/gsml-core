package com.asanasoft.gsml.client.token.jwt.impl

import com.asanasoft.gsml.client.init.Environment
import com.nimbusds.jose.crypto.AESDecrypter
import com.nimbusds.jwt.EncryptedJWT
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

open class JweToken : JwsToken() {
    private var decrypted = false

    open fun decrypt() : Boolean {
        decrypted = !(parsedToken is EncryptedJWT)

        if (!decrypted) {
            /**
             * encrypted is set here because ALL Jwt tokens are assumed to be encrypted, whether or not they actually
             * are. If the Jwt is actually a Jwe, then the flag is set to TRUE. Clients of this token can query the
             * value if their processing requires a token to be of TYPE "encrypted".
             */
            this.encrypted = true

            var secretKey : String = Environment.getProperty("gsmlSecretKey") ?: ""

            try {
                with(parsedToken as EncryptedJWT) {
                    this.decrypt(AESDecrypter(secretKey.encodeToByteArray()))

                    /**
                     * If this is indeed an encrypted token, then check if the payload is signed, if so, then assign
                     * the parsedToken to the payload as a SignedJWT so that the <code>verfy()</code> function can validate
                     * as a signed token. <code>parsedToken</code> is an internal and interim representation of the token, and
                     * only used for the unmarshalling process. Unmarchalling again with the marshalled string will
                     * recreate the parsedToken.
                     */
                    if (this.header.contentType.equals("JWS")) parsedToken = this.payload.toSignedJWT()
                }
                decrypted = true
            }
            catch (e : Exception) {
                logger.error {
                    "An error occured decrypting token: ${e.message}"
                }
            }
        }

        return decrypted
    }

    override fun validate() : Boolean {
        var result = false

        if (decrypt()) result = super.validate()

        return result
    }
}