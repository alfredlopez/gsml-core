package com.asanasoft.gsml.client

import com.asanasoft.gsml.client.GSMLConstants.TOKEN_MANAGER
import com.asanasoft.gsml.client.communication.Visitor
import com.asanasoft.gsml.client.communication.broadcast.EventBroadcasterHub
import com.asanasoft.gsml.client.communication.listener.BroadcastListenerHub
import com.asanasoft.gsml.client.events.*
import com.asanasoft.gsml.client.events.EventType.*
import com.asanasoft.gsml.client.events.listener.EventListener
import com.asanasoft.gsml.client.init.Environment
import com.asanasoft.gsml.client.token.*
import com.asanasoft.gsml.client.token.jwt.impl.TokenTypes
import com.asanasoft.gsml.client.utility.Injector
import com.asanasoft.gsml.client.utility.jsonFormat
import com.asanasoft.gsml.client.utility.launchPeriodicAsync
import com.asanasoft.gsml.exception.TokenManagerException
import com.asanasoft.gsml.server.TokenIssuerService
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import mu.KotlinLogging
import org.slf4j.MDC
import java.time.Instant
import java.util.*

private var logger = KotlinLogging.logger {}

/**
 * TokenManager is the entry point to the Security Utilities middleware, on
 * the client side.
 *
 * @constructor Create empty Token manager
 */
//@kotlin.js.ExperimentalJsExport
open class TokenManager : EventListener, Visitor {
    //This is the backing field for the "accessToken" property...
    private var _accessToken : AccessToken? = null
    private var identityToken : IdentityToken? = null
    private var refreshToken : RefreshToken? = null
    private var refreshTokenId : String? = null

    /**
     * setRefreshTimer is configurable from tokenManager.properties file. If the file does not exist, or the property
     * is not set, this value defaults to true.
     */
    private var setRefreshTimer = true
    private var maxIsValidDelay = 3 //<-- will block the call for 3 x isValidDelayFreq milliseconds...
    private var isValidDelayFreq = 500L //delay in milliseconds

    private val OBJECT_ID = "object-id"

    //TokenManager will share the valid state with the Identity token...
    private var inValidState : Boolean = false
        set(value) {
            val notInValidState = !field
            field = value

            if (notInValidState && value) {
                /**
                 * Registering here in case and invalid TokenManager is "reused" with a valid AccessToken...
                 */
                BroadcastListenerHub.register(this)
            }
        }

    /**
     * This field shouldn't be necessary, but getting the Identity token
     * from the current implementation of the Security Utilities\
     * middleware, delivers a different structure than what is received
     * in a Access Token...
     *
     * In the future, we may want to homogenize the structure of Identity/Refresh Token payload
     */
    private var initialIdentityToken : InitialIdentityToken? = null

    private var liveTokens : MutableMap<String, LiveToken>
    private var _context : MutableMap<String, String>
    private var listeners : MutableList<EventListener>

    private var registered : Boolean = false

    var accessToken : String? = null
        @Throws(TokenManagerException::class)
        set(value) {
            try {
                inValidState = false

                identityToken = null
                refreshToken = null
                refreshTokenId = null

                field = value

                value?.let {
                    val initialAccessToken = Injector.inject(AccessToken::class.java)

                    if (initialAccessToken.next != null) {
                        _accessToken = initialAccessToken.fromMarshalled(value) as AccessToken
                    }
                    else {
                        _accessToken = initialAccessToken
                        _accessToken!!.marshalled = value
                    }

                    _context.clear()

                    _context.putAll(_accessToken!!.getContext()!!)

                    identityToken = _accessToken?.identityToken
                    refreshToken = _accessToken?.refreshToken
                    refreshTokenId = _accessToken?.refreshTokenId

                    val refreshTime = (identityToken!!.expiresAt.time - identityToken!!.issuedAt.time)

                    logger.trace("Refresh Token ID = $refreshTokenId")

                    startTokenRefreshTimer(refreshTime)

                    inValidState = true

                    this._context.clear()

                    _accessToken?.populateContext(_context)
                    identityToken?.populateContext(_context)

                    register()
                }
            }
            catch (e : Exception) {
                inValidState = false
                logger.trace("TokenManager valid state set to false: ${e.stackTraceToString()}")
                throw TokenManagerException("Error setting AccessToken", this).initCause(e)
            }
        }

    var isValid : Boolean = false
        get() = runBlocking {
            accessing.withLock {
                field = false

                if (identityToken != null) {
                    field = inValidState
                }

                return@runBlocking field
            }
        }
        set(newValue) {
            noop(newValue)
        }

    var principal : String?
        get() {
            return identityToken?.principal
        }
        set(newValue) {
            noop(newValue)
        }

    var context : Map<String, String>?
        get() {
            return _context
        }
        set(newValue) {
            noop(newValue)
        }

    var id : String?
        get() {
            return if (this.refreshTokenId != null) this.refreshTokenId else this.toString()
        }
        set(newValue) {
            noop(newValue)
        }

    //Allow this TokenManager to manage more than one token...
    @Deprecated("This feature will soon be removed!", level = DeprecationLevel.WARNING)
    var multiTokenMode  : Boolean = false

    private var accessing = Mutex() //Mutex Flag indicating this TokenManager is notifying listeners

    /**
     * Creating a Scoped SupervisorJob for notification and stuff...
     */
    private var supervisedJob = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.Default + supervisedJob)

    init {
        logger.debug("Initializing TokenManager Instance...")


        liveTokens  = mutableMapOf() // HashMap<String, LiveToken>()
        _context    = mutableMapOf() //HashMap<String, String>()
        listeners   = mutableListOf()

        val tokenManagerProperties = Environment.getProperties("tokenManager.properties", true)

        setRefreshTimer = (tokenManagerProperties?.getProperty("setRefreshTimer") == null || tokenManagerProperties.getProperty("setRefreshTimer").toBoolean())

        logger.info("Will refresh tokens: $setRefreshTimer")
    }

    /**
     * Helper function for Java
     */
    open fun createIdentityToken(
        sourceSystem : String,
        sessionTimeout : Int,
        refreshTimeout : Int,
        principalId : String,
        additionalData : MutableMap<String, String>
    ) {
        createIdentityToken(sourceSystem, sessionTimeout, refreshTimeout, principalId, additionalData, false, null)
    }

    open fun createIdentityToken(
        sourceSystem : String,
        sessionTimeout : Int,
        refreshTimeout : Int,
        principalId : String,
        additionalData : MutableMap<String, String>,
        handler : ((Result<String>) -> Unit)? = null
    ) {
        createIdentityToken(sourceSystem, sessionTimeout, refreshTimeout, principalId, additionalData, false, handler)
    }

    open fun createIdentityToken(
        sourceSystem : String,
        sessionTimeout : Int,
        refreshTimeout : Int,
        principalId : String,
        additionalData : MutableMap<String, String>,
        encrypted : Boolean = false
    ) {
        createIdentityToken(sourceSystem, sessionTimeout, refreshTimeout, principalId, additionalData, encrypted, null)
    }

    open fun createIdentityToken(
        sourceSystem : String,
        sessionTimeout : Int,
        refreshTimeout : Int,
        principalId : String,
        additionalData : MutableMap<String, String>,
        encrypted : Boolean = false,
        handler : ((Result<String>) -> Unit)? = null
    ) {
        preCreateIdentityToken(sourceSystem, sessionTimeout, refreshTimeout, principalId, additionalData)

        logger.debug("Creating Identity Token using args...")

        val properties = Environment.getProperties("accessToken.properties", true)
        val principalIdName = properties?.getProperty("principalIdName") ?: "principalId"

        val tokenType = if (encrypted) TokenTypes.ENCRYPTED else TokenTypes.SIGNED

        additionalData.putIfAbsent(principalIdName, principalId)

        val jsonObject2 = buildJsonObject {
            put("legacy_system_id", sourceSystem)
            put("session_timeout", sessionTimeout.toString())
            put("refresh_timeout", refreshTimeout.toString())
            put("requested_token_type", tokenType)
            putJsonObject("legacy_keys") {
                for (key in additionalData.keys) {
                    this.put(key, additionalData[key])
                }
            }
        }

        createIdentityToken(jsonObject2.toString(), handler)
    }

    /**
     * Helper function for Java
     */
    open fun createIdentityToken(principal : String) {
        createIdentityToken(principal, null)
    }

    open fun createIdentityToken(principal : String, handler : ((Result<String>) -> Unit)? = null) {
        val instance = this

        MDC.put(OBJECT_ID, this.toString())

        runBlocking(MDCContext()) {
            var result : Result<String>?
            var notification : Event

            logger.debug("Creating Identity Token using principal JSON...:\n $principal")

            logger.debug("Getting Identity Token from service...")

            val identityTokenRequest = jsonFormat.decodeFromString<IdentityTokenRequest>(principal)
            val initialIdentityTokenJob = async {
                TokenIssuerService.getIdentityToken(identityTokenRequest)
            }

            try {
                initialIdentityToken = initialIdentityTokenJob.await()

                if (initialIdentityToken != null) {
                    //logger.debug("Received Identity Token from service:\n${initialIdentityToken?.token?.subSequence(0,10)}")

                    identityToken   = Injector.inject(IdentityToken::class.java)
                    refreshToken    = Injector.inject(RefreshToken::class.java)

                    logger.debug("Setting Identity Token...")

                    identityToken!!.marshalled  = initialIdentityToken?.token
                    refreshToken!!.marshalled   = initialIdentityToken?.refresh_token
                    refreshTokenId              = initialIdentityToken?.refresh_token_jti

                    postCreateIdentityToken()

                    /**
                     * Compute the refresh time based on NOW and NOT the issued time, just in case
                     * the IDP sends back the original, unexpired, token...
                     */
                    val refreshTime = (identityToken!!.expiresAt.time - Instant.now().toEpochMilli())

                    logger.debug("Setting REFRESH time to $refreshTime milliseconds...")

                    startTokenRefreshTimer(refreshTime)
                    inValidState = true

                    _context.clear()

                    identityToken?.populateContext(_context)

                    result = Result(value = "IdentityToken acquired")
                    notification = Success("IdentityToken acquired")
                    notify(notification)
                }
                else {
                    throw TokenManagerException("No IdentityToken returned from IDP!!", instance)
                }
            }
            catch (t : Throwable) {
                inValidState = false
                logger.error("A throwable occurred in createIdentityToken:", t)
                notification = TokenError(t.message)
                result = Result(cause = t)
                notify(notification)
                invalidate(false)
            }
            catch (e : Exception) {
                inValidState = false
                logger.error("An exception occurred in createIdentityToken:", e)
                notification = TokenError(e.message)
                result = Result(cause = e)
                notify(notification)
                invalidate(false)
            }

            handler?.invoke(result!!)
        }

        MDC.remove(OBJECT_ID)
    }

    /**
     * Helper function for Java
     */
    open fun createAccessToken(destinationId : String, language : String, context : Map<String, String>) : String? {
        return createAccessToken(destinationId, language, context, false)
    }

    open fun createAccessToken(
        destinationId : String,
        language : String,
        context : Map<String, String>,
        handler : ((Result<String>) -> Unit)? = null
    ) : String? {
        return createAccessToken(destinationId, language, context, false, handler)
    }

    open fun createAccessToken(destinationId : String, language : String, context : Map<String, String>, encrypted : Boolean = false) : String? {
        return createAccessToken(destinationId, language, context, encrypted, null)
    }

    /**
     * If you always use the same parameters to create a AccessToken, call this method to return the previously
     * created one, or a new one, using the previous parameters, if the current one has expired.
     */
    open fun getLastAccessToken() : String? {
        var result : String? = null

        _accessToken?.let {
            result = getLastAccessToken(it.encrypted)
        }

        return result
    }

    open fun getLastAccessToken(encrypted : Boolean) : String? {
        var result : String? = null
        val secs15 : Long = 15 * 1000

        _accessToken?.let {
            //Check that the token is still valid up to 15 sec before expiration, unless the encrypted status changed...
            result = if (it.encrypted == encrypted && it.expiresAt.after(Date(System.currentTimeMillis() + secs15))) {
                logger.debug("Returning previous AccessToken...")
                it.marshalled
            }
            else {
                logger.debug("Returning new AccessToken...")
                createAccessToken(it.destination, it.language, it.getContext()!!, encrypted)
            }
        }

        return result
    }

    open fun createAccessToken(
        destinationId : String,
        language : String,
        context : Map<String, String>,
        encrypted : Boolean = false,
        handler : ((Result<String>) -> Unit)? = null
    ) : String? {
        val instance = this

        var result : Result<String>? = null
        val localContext = context.ifEmpty { mapOf("" to "") }

        MDC.put(OBJECT_ID, this.toString())

        runBlocking(MDCContext()) {

            val tokenType = if (encrypted) TokenTypes.ENCRYPTED else TokenTypes.SIGNED

            var notification : Event

            preCreateAccessToken(destinationId, language, context)

            logger.info("Creating Access Token...")

            val identityTokenString = identityToken?.marshalled!!
            val refreshTokenString = refreshToken?.marshalled!!

            try {
                if (inValidState) {
                    logger.info("Requesting Access Token from service...")

                    val accessTokenString = withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
                        TokenIssuerService.getAccessToken(identityTokenString, destinationId, language, localContext, refreshTokenString, tokenType)
                    }

                    if (accessTokenString != null) {
                        logger.debug("Received Access Token from service: ${accessTokenString.subSequence(0, 10)}")

                        accessToken = accessTokenString

                        val refreshTime = (identityToken!!.expiresAt.time - identityToken!!.issuedAt.time)

                        logger.debug("Setting REFRESH time to $refreshTime milliseconds...")
                        logger.debug("      Expires on {}", Date(identityToken!!.expiresAt.time))

                        postCreateAccessToken()
                        startTokenRefreshTimer(refreshTime)

                        result = Result(value = accessToken!!)
                        notification = Success("Access Token acquired")
                        notification.put("accessToken", accessToken)
                        notify(notification)
                    }
                    else {
                        throw TokenManagerException("No access token from TokenIssuer or RefreshToken expired. Please check TokenIssuer for possible errors!", instance)
                    }
                }
                else {
                    throw TokenManagerException("TokenManager is invalid", instance)
                }
            }
            catch (t : Throwable) {
                logger.error("A throwable occurred in createAccessToken:", t)
                notification = TokenError(t.message)
                result = Result(cause = t)
                notify(notification)
                invalidate(false)
            }
            catch (e : Exception) {
                logger.error("An exception occurred in createAccessToken:", e)
                notification = TokenError(e.message)
                result = Result(cause = e)
                notify(notification)
                invalidate(false)
            }

            handler?.invoke(result!!)
        }

        MDC.remove(OBJECT_ID)

        return result?.value
    }

    /**
     * Register listener
     *
     * @param listener
     */
    override fun register(listener : EventListener) {
        val event : Register?

        if (!listeners.contains(listener)) {
            listeners.add(listener)
            event = Register("Registering", this)

            scope.launch {
                listener.eventTriggered(event) //Notify listener with a Register event...
            }
        }
    }

    override fun unregister(listener : EventListener) {
        val event : Unregister?
        if (!listeners.contains(listener)) {
            listeners.remove(listener)
            event = Unregister("Unregistering", this)

            scope.launch {
                listener.eventTriggered(event) //Notify listener with a Register event...
            }
        }
    }

    override fun notify(event : Event) {
        logger.debug("Notifying listeners for TokenManager: ${this.id}")

        listeners.forEach {
            scope.launch {
                preNotify(event, it)
                it.eventTriggered(event)
                postNotify(event, it)
            }
        }
    }

    open fun invalidate() {
        MDC.put(OBJECT_ID, this.toString())
        invalidate(true, null)
        MDC.remove(OBJECT_ID)
    }

    open fun invalidate(handler : ((Result<String>) -> Unit)? = null) {
        invalidate(true, handler)
    }

    open fun getContextValue(key : String) : String? {
        return context?.get(key)
    }

    /**
     * Use of this method should be restricted to environments where session caching cannot include caching of the
     * TokenManager instance (e.g. NodeJS/Redis). In this case, a marshalled encrypted AccessToken would be persisted
     * along with the session. When the session becomes alive again (pulled from cache, for example), a TokenManager can
     * be "reconstituted" from this previous AccessToken. The refresh token ID of the encrypted Access Token must be
     * known in order to use this feature.
     *
     * If the TokenManager is in *multiTokenMode*, it will return a new TokenManager instance instead of *this*
     */
    open fun reconstitute(refreshTokenId : String, oldEncryptedAccessToken : String) : TokenManager? {
        var result : TokenManager? = null
        val validatedAccessToken = Injector.inject(ValidatedAccessToken::class.java)
        val oldAccessToken = ValidatedAccessToken(validatedAccessToken)

        try {
            oldAccessToken.marshalled = oldEncryptedAccessToken

            if (oldAccessToken.refreshTokenId == refreshTokenId) {
                val miniMe = this

                miniMe._accessToken = oldAccessToken
                miniMe.identityToken = oldAccessToken.identityToken
                miniMe.refreshToken = oldAccessToken.refreshToken
                miniMe.inValidState = oldAccessToken.isValid
                miniMe.refreshTokenId = refreshTokenId

                miniMe.identityToken?.populateContext(miniMe._context)
                miniMe._accessToken?.populateContext(miniMe._context)

                val refreshTime = (identityToken!!.expiresAt.time - identityToken!!.issuedAt.time)

                logger.debug("Setting Mini Me's REFRESH time to $refreshTime milliseconds...")

                miniMe.startTokenRefreshTimer(refreshTime)

                miniMe.inValidState = true

                result = miniMe
            }
        }
        catch (e : Exception) {
            logger.error("An error occurred in reconstitute:", e)
        }

        return result
    }

    override fun eventTriggered(event : Event?) {
        if (event != null) {
            MDC.put(OBJECT_ID, this.toString())

            logger.debug("Event: {} / {}", event.type, event.message)

            when (event.type) {
                REVOKE -> {
                    logger.debug("Checking managed tokens for revocation...")

                    val revokedTokens = event.get("revokedTokens") as RevokedTokens
                    logger.debug("revokedTokens size: " + revokedTokens.refresh_tokens.size)
                    for (refreshToken in revokedTokens.refresh_tokens) {
                        if (liveTokens.containsKey(refreshToken)) { // NOSONAR
                            logger.debug("Calling invalidate ...")
                            invalidate(false)
                        }
                    }
                }
                REFRESH -> {
                    if ("CHECK_TOKEN".equals(event.message, true)) {
                        refreshAccessToken()

                        //Allow the host application to react to this...
                        notify(event)
                    }
                }
                ERROR -> {
                    noop()
                }
                WAKE -> pass
                else -> pass
            }

            MDC.remove((OBJECT_ID))
        }
    }

    open fun discard() {
        runBlocking {
            accessing.withLock {
                _context.clear()
                listeners.clear()
                liveTokens.forEach{
                    it.value.job.cancel()
                }

                liveTokens.clear()

                identityToken?.discard()
                refreshToken?.discard()
                _accessToken?.discard()

                inValidState    = false
                accessToken    = null
                identityToken   = null
                refreshToken    = null
                refreshTokenId  = null
                _accessToken   = null
            }
        }
    }

    override fun flush() {
        noop()
    }

    fun finalize() {
        logger.debug("Finalizing TokenManager: $id")
        discard()
    }

    protected open fun register() {
        if (!registered) {
            BroadcastListenerHub.register(this)
            registered = true
        }
    }
    protected open fun refreshAccessToken() : String? {
        val instance = this
        val result : String?

        runBlocking {
            accessing.withLock {
                with(instance) {
                    logger.debug("Refreshing Access/Identity Token...${this.id}")


                    var contextKeys = mutableMapOf("" to "")

                    lateinit var destinationId : String
                    lateinit var lang : String

                    var encrypted = false

                    if (_accessToken != null) {
                        contextKeys.putAll(_accessToken!!.getContext()!!)

                        destinationId = _accessToken!!.destination
                        lang = _accessToken!!.language
                        encrypted = _accessToken!!.encrypted
                    }
                    else {
                        destinationId = "None"
                        lang = "en"
                        contextKeys = mutableMapOf("" to "") //The API requires a context. It should default to this if empty.
                    }

                    result = createAccessToken(
                        destinationId,
                        lang,
                        contextKeys,
                        encrypted
                    ) {
                        if (it.isFailure) {
                            logger.error("An error occurred creating a new access token for REFRESH event!", it.cause)
                            notify(TokenError(it.cause?.message))
                        }
                    }

                    if (!result.isNullOrEmpty()) {
                        accessToken = result
                    }
                }
            }
        }

        return result
    }

    protected open fun invalidate(broadcast : Boolean, handler : ((Result<String>) -> Unit)? = null) {
        val instance = this

        runBlocking {
            accessing.withLock {
                with(instance) {
                    logger.info("Invalidating...")
                    val result : Result<String>

                    if (!inValidState && broadcast) {
                        logger.error("TokenManager is already invalidated. No action taken.")
                        result = Result(
                            cause = TokenManagerException(
                                "TokenManager is already invalidated.",
                                instance
                            )
                        )
                        handler?.invoke(result)
                    }

                    if (inValidState) {
                        inValidState = false

                        val event = Revoke(
                            message = "Refresh Token with id=${refreshTokenId} has been revoked"
                        )
                        event.put("revokedToken", refreshToken?.marshalled!!)
                        event.put("revokedTokenId", refreshTokenId)

                        liveTokens.remove(refreshTokenId)?.job?.cancel() //cancel the refresh job

                        notify(event)

                        if (broadcast) {
                            logger.info("Broadcasting invalidate event for id: $refreshTokenId from: ${Environment.getProperty("gsml_app")}")
                            EventBroadcasterHub.broadcast(event, handler)
                        }

                        BroadcastListenerHub.unregister(instance)
                    }

                }
            }

            discard()
        }
    }

    protected open fun preNotify(event : Event, eventListener : EventListener) {
        event.put(TOKEN_MANAGER, this)
    }

    protected open fun postNotify(event : Event, eventListener : EventListener) {
        noop()
    }

    protected open fun preCreateAccessToken(destinationId : String, language : String, context : Map<String, String>) {
        noop()
    }

    protected open fun postCreateAccessToken() {
        register()
    }

    protected open fun preCreateIdentityToken(
        sourceSystem : String,
        sessionTimeout : Int,
        refreshTimeout : Int,
        principalId : String,
        additionalData : MutableMap<String, String>
    ) {
        noop()
    }

    protected open fun postCreateIdentityToken() {
        register()
    }

    protected open fun startTokenRefreshTimer(pollTime : Long) {
        lateinit var event : Event
        lateinit var refreshTokenJob : Job

        logger.debug("In startTokenRefreshTimer...")
        logger.debug("    Polling = $pollTime ...")

        /**
         * Don't set a timer if pollTime is less than or equal to zero. This allows Tokens to be validated that don't
         * require management. Think "AccessTokens"...
         * REFRESH events usually re-validates tokens. <code>validate()</code> now re-validates, so this should be safe.
         */
        if ((pollTime > 0) && setRefreshTimer) {
            logger.trace { "AccessToken / RefreshToken: $accessToken / $refreshTokenId" }

            //Create a new instance of EVENT with the current RefreshTokenId...
            logger.debug("Creating CHECK_TOKEN Event...")
            event = Refresh("CHECK_TOKEN")
            event.put("refreshTokenId", "$refreshTokenId")  //effectively, make a copy of the id...
            event.put("accessToken", _accessToken)

            if (liveTokens.containsKey(refreshTokenId)) {
                liveTokens[refreshTokenId]?.job?.cancel()
                liveTokens.remove(refreshTokenId)
            }

            logger.debug("Creating Job...")
            refreshTokenJob = CoroutineScope(Dispatchers.IO).launchPeriodicAsync(pollTime, true) {
                eventTriggered(event)
            }

            logger.debug("Saving Job...")
            liveTokens[refreshTokenId!!] =
                LiveToken(_accessToken, refreshTokenJob)
        }

        logger.debug("Leaving startTokenRefreshTimer()...")
    }
}
