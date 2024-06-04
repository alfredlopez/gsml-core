package com.asanasoft.gsml.client.token.jwt.impl

import com.asanasoft.gsml.client.Keys
import com.asanasoft.gsml.client.communication.listener.BroadcastListenerHub
import com.asanasoft.gsml.client.events.Event
import com.asanasoft.gsml.client.events.listener.FlowListener
import com.asanasoft.gsml.client.noop
import com.asanasoft.gsml.client.token.jwt.Jwk
import com.asanasoft.gsml.client.token.jwt.JwkProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import mu.KotlinLogging
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*

private val logger = KotlinLogging.logger {}

open class SUJwkProvider : JwkProvider, FlowListener<Keys> {
    protected var keys : MutableMap<String, Jwk> = HashMap()
    private val MAX_RETRIES_OR_FAIL = 2 //Retry a method body a maximum of two tries...
    protected val mucinex = Mutex()

    override var flowListener : Flow<Keys>? = null

    init {
        logger.info("Initializing JwkProvider...")
        BroadcastListenerHub.register(this)
        logger.info("JwkProvider registered...")
    }

    protected fun publicKeyToRSAPk(publicKey : String) : RSAPublicKey {
        val result : RSAPublicKey
        val decoded : ByteArray = Base64.getDecoder().decode(publicKey)
        val spec = X509EncodedKeySpec(decoded)
        val kf = KeyFactory.getInstance("RSA")
        result = kf.generatePublic(spec) as RSAPublicKey
        return result
    }

    override fun get(keyId : String) : Jwk = runBlocking {
        /**
         * Because it is possible that the current list of keys have not been fetched,
         * we will suspend the call and collect the keys from the flow owned by the listener.
         * This will happen if the keyId is not initially found. Once the keys are fetched,
         * we will retry the operation. If the failure persists, then it is reported.
         *
         * Assumptions:
         *   1. Once the fetch has happened, "key not found" error should abate until after key rotation
         *   2. Because of #1, there is no need for a "dirty flag" or such other mechanism.
         */
        var retry = 0
        var result : Jwk? = null

        mucinex.withLock {
            do {
                if (keys.containsKey(keyId)) {
                    result = keys[keyId]
                    retry = MAX_RETRIES_OR_FAIL
                }
                else {
                    /**
                     * Though the mutex is locked and any subsequent calls are "suspended", we don't
                     * want to re-run this block, so we will redirect subsequent calls to a waiting
                     * area...
                     *
                     * NOTE: Through testing, it seems if two or more coroutines are fired very close
                     * to each other, there is a chance that each can run this block of code. This is
                     * slight and, statistically,
                     *
                     */
                    retry++
                    logger.info("KeyId not found in cache. Collecting from flow...")
                    flowListener?.collect {
                        logger.debug("Calling fillCache...")
                        fillCache(it)
                    }
                    logger.info("Keys collected...")
                }
            } while (retry < MAX_RETRIES_OR_FAIL)
        }

        //I hate this next line of code, but...
        if (result == null) throw SigningKeyNotFoundException("Key Not Found!!")

        return@runBlocking result!!
    }

    protected fun fillCache(rotatedKeys : Keys) {
        logger.info("Filling cache...")
        for (key in rotatedKeys.keys) {
            if (!keys.containsKey(key.kid)) {
                val additionalAttributes = HashMap<String, Any>()
                val pubKey = publicKeyToRSAPk(key.publicKey)

                additionalAttributes["n"] = pubKey.modulus.toString()
                additionalAttributes["e"] = pubKey.publicExponent.toString()
                additionalAttributes["origPubKey"] = pubKey

                val jwk = DefaultJwk(
                    key.kid,
                    "RSA",
                    "RSA256",
                    "sig",
                    Collections.singletonList("sig"),
                    "",
                    Collections.singletonList(""),
                    "",
                    additionalAttributes
                )

                keys[key.kid] = jwk
            }
        }
        logger.info("Cache filled...")
    }

    /**
     * We're undefining this method...
     */
    override fun eventTriggered(event : Event?) {
        noop()
    }
}