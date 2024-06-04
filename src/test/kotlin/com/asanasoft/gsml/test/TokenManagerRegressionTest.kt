package com.asanasoft.gsml.test

import com.asanasoft.gsml.client.communication.listener.BroadcastListenerHub
import com.asanasoft.gsml.client.events.Success
import com.asanasoft.gsml.client.token.AccessToken
import com.asanasoft.gsml.client.utility.Injector
import com.asanasoft.gsml.exception.TokenManagerException
import com.manulife.gsml.test.TokenManagerListener2
import com.asanasoft.gsml.client.Result
import com.asanasoft.gsml.client.events.listener.EventListener

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.junit.Test
import org.slf4j.MDC
import java.time.Instant
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource
import kotlin.time.TimeMark

private val logger = KotlinLogging.logger {}

class TokenManagerRegressionTest {
    var mucinex = Mutex()

    var someList = mutableListOf<EventListener>()

    var RANDOM_MAX_NUMBER = 0
    val tokenArray = ArrayList<com.asanasoft.gsml.client.TokenManager>()
    val startTime = Instant.now()
    var oldAccessToken : String? = null
    var ct : String? = null

    var supervisedJob = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.Default + supervisedJob)

    suspend fun testSignedToken() {
        logger.info("Starting testSignedToken...")
        testCreateToken()
    }

    suspend fun testEncryptedToken() {
        logger.info("Starting testEncryptedToken...")
        testCreateToken(true)
    }

    @Test
    fun testInvalidateToken() {
        logger.info("Starting testInvalidateToken...")
        for (tokenManager in tokenArray) {
            tokenManager.invalidate()
        }
    }

    suspend fun testCreateToken(encrypted : Boolean = false) {
        logger.info("Creating three IdentityTokens in parallel...")

        var tokenManager1 = TestingTokenManager("TokenManager 1")
        var tokenManager2 = TestingTokenManager("TokenManager 2")
        var tokenManager3 = TestingTokenManager("TokenManager 3")
        var tokenManager4 = TestingTokenManager("TokenManager 4")
        var tokenManager5 = TestingTokenManager("TokenManager 5") //create this TokenManager, but don't assign any tokens...

        tokenManager1.register(TokenManagerListener2())
        tokenManager1.register(TokenManagerListener2())
        tokenManager1.register(TokenManagerListener2())

        tokenManager2.register(TokenManagerListener2())

        tokenManager4.register(TokenManagerListener2())

        val job1 = scope.launch {
            MDC.put("object-id", tokenManager1.toString())
//            tokenManager1.createIdentityToken("SLX", 300, 3600, "2000226", mutableMapOf("sso_token" to "6ed74503-60ba-49c7-904b-88acce7a578d", "indCustId" to "2000888"), encrypted) {
            tokenManager1.createIdentityToken("Source", 300, 3600, "2000226", mutableMapOf("indCustId" to "2000888"), encrypted) {
                if (it.isSuccess) {
                    logger.info("Succeeded in creating an encrypted IdentityToken1:${tokenManager1.name}")
                }
                else {
                    logger.info("Failed in creating an encrypted IdentityToken1:${tokenManager1.name}")
                }
            }
            MDC.remove("object-id")
        }

        val job2 = scope.launch {
            MDC.put("object-id", tokenManager2.toString())
            tokenManager2.createIdentityToken("Source", 300, 3600, "2000226", HashMap(), encrypted) {
                if (it.isSuccess) {
                    logger.info("Succeeded in creating an encrypted IdentityToken2:${tokenManager2.name}")
                }
                else {
                    logger.info("Failed in creating an encrypted IdentityToken2:${tokenManager2.name}")
                }
            }
            MDC.remove("object-id")
        }
//
//        val job3 = scope.launch {
//            MDC.put("object-id", tokenManager3.toString())
//            tokenManager3.createIdentityToken("SLX", 180, 3600, "2000226", HashMap(), encrypted) {
//                if (it.isSuccess) {
//                    logger.info("Succeeded in creating an encrypted IdentityToken3:${tokenManager3.id}")
//                }
//                else {
//                    logger.info("Failed in creating an encrypted IdentityToken3")
//                }
//            }
//            MDC.remove("object-id")
//        }

        logger.info("Waiting for all IdentityTokens to be created...")

        joinAll(job1, job2)

        logger.info("Creating a AccessToken...")
        var success = false
        var ct1 = tokenManager1.createAccessToken("DEST", "en", mapOf("Hello" to "World", "Foo" to "Bar"), encrypted) {
            if (it.isSuccess) {
                logger.info("Succeeded in creating an encrypted AccessToken for ${tokenManager1.name}")
                success = true
            }
            else {
                logger.info("Failed in creating an encrypted AccessToken for ${tokenManager1.name}")
            }
        }

        printContext(tokenManager1)

        if (success) {
            logger.info("Created AccessToken...${ct1?.substring(0, 10)}")
            logger.info("Assigning AccessToken to second TokenManager...")

            tokenManager4.accessToken = ct1
            logger.info("tokenManager4.isValid = ${tokenManager4.isValid} / ${tokenManager4.id}")
//            tokenArray.add(tokenManager2)

            printContext(tokenManager4)
        }

//        tokenManager1.invalidate() {
//            if (it.isSuccess) {
//                logger.info("Successfully invalidated: ${tokenManager1.id}")
//            }
//            else {
//                logger.info("Unsuccessfully invalidated: ${tokenManager1.id}")
//            }
//        }

        tokenArray.add(tokenManager1)
        tokenArray.add(tokenManager2)
        tokenArray.add(tokenManager4)

        oldAccessToken = ct1

        logger.info("TokenManager1 and TokenManager4 should have the same id: ${tokenManager1.id} / ${tokenManager4.id}")
    }

    fun testCreateIdentityTokenWithNonce(tokenManager : com.asanasoft.gsml.client.TokenManager?, nonce : String = "", custId : String = "") : com.asanasoft.gsml.client.TokenManager {
        val _tokenManager : com.asanasoft.gsml.client.TokenManager
        val identityContext = HashMap<String, String>()
        identityContext.put("indCustId", custId)
        identityContext.put("ciamNonceUuid", nonce)

        logger.info("*********** Calling createIdentityToken...1 ************")

        _tokenManager = if (tokenManager != null) tokenManager else com.asanasoft.gsml.client.TokenManager()

        _tokenManager.createIdentityToken("SLX", 60, 7200, "2000224", identityContext) {
            if (it.isSuccess) {
                logger.info("IdentityToken1 acquired...")
                logger.info("Calling createAccessToken...")
                _tokenManager.createAccessToken("Node", "en", HashMap()) {
                    if (it.isSuccess) {
                        logger.info("AccessToken1 acquired...")
                        ct = it.value
                    }
                    else {
                        logger.error("A failure occurred in acquiring a AccessToken1:", it.cause)
                    }
                }
                logger.info("Printing the accessToken1 = ${ct?.substring(0, 10)}")
            }
            else {
                logger.error("A failure occurred acquiring IdentityToken1:", it.cause)
                _tokenManager.invalidate() //TokenManager should be invalidated already. This call will generate an exception...
            }
        }

        return _tokenManager
    }

    @Test
    @OptIn(ExperimentalTime::class)
    fun fullTest() {
        runBlocking {
            logger.info("Starting test...")

            val tokenManager = com.asanasoft.gsml.client.TokenManager()
            val tokenManager2 = com.asanasoft.gsml.client.TokenManager()
            val tokenManager3 = com.asanasoft.gsml.client.TokenManager()
            val tokenManager4 = com.asanasoft.gsml.client.TokenManager()

            var ct2 : String? = null

            logger.info("=================================================")


            try {
                testCreateIdentityTokenWithNonce(tokenManager, "48556792-c306-434c-a31d-9711b6fa21a8", "107657887")

                /*
                The equivalent in Java is...

                tokenManager.createIdentityToken("SLX", 60, 7200, "2000224", new HashMap<String, String>(), it -> {
                    if (it.isSuccess) {
                        logger.info("IdentityToken1 acquired...");
                        logger.info("Calling createAccessToken...");
                        tokenManager.createAccessToken("Node", "en", HashMap()) {
                            if (it.isSuccess) {
                                logger.info("AccessToken1 acquired...");
                                ct = it.value;
                            } else {
                                logger.error("A failure occurred in acquiring a AccessToken1:", it.cause);
                            }
                        }
                        logger.info("Printing the accessToken1 = " + ct.substring(0, 10));
                    } else {
                        logger.error("A failure occurred acquiring IdentityToken1:", it.cause);
                        tokenManager.invalidate(); //Do NOT go any further...
                    }

                    return null; // <-- unfortunately, this is required (stoopid Java!)
                }
                 */

                logger.info("*********** Calling createIdentityToken...2 ************")

                tokenManager2.createIdentityToken("SLX", 60, 3600, "2000226", HashMap()) {
                    if (it.isSuccess) {
                        logger.info("IdentityToken2 acquired...")
                        logger.info("Calling createAccessToken...")
                        tokenManager2.createAccessToken("Node", "en", HashMap()) {
                            if (it.isSuccess) {
                                logger.info("AccessToken2 acquired...")
                                ct2 = it.value
                            }
                            else {
                                logger.error("A failure occurred in acquiring a AccessToken2:", it.cause)
                            }
                        }
                        logger.info("Printing the accessToken2 = ${ct2?.substring(0, 10)}")
                    }
                    else {
                        logger.error("A failure occurred acquiring IdentityToken2:", it.cause)
                        tokenManager2.invalidate() //Do go any further...
                    }
                }

                logger.info("*********** Calling createIdentityToken...3 ************")

                tokenManager3.createIdentityToken("MPS", 30, 3600, "2000000", mutableMapOf("indCustId" to "2000888")) {
                    if (it.isSuccess) {
                        logger.info("IdentityToken3 acquired...")
                    }
                    else {
                        logger.error("A failure occurred acquiring IdentityToken3:", it.cause)
                        tokenManager3.invalidate() //Do go any further...
                    }
                }

                logger.info("*********** Calling createIdentityToken...4 ************")

                //Let's make a "defaul call" to createIdentityToken...
                tokenManager4.createIdentityToken("MPS", 15, 3600, "2000000", mutableMapOf("indCustId" to "101391526"))

                /**
                 * ct3 is declared here to show how scoped variables are accessed...
                 */
                var ct3 : String? = null

                logger.info("Calling the *abi-normal* createAccessToken with callBack...")
                ct3 = tokenManager3.createAccessToken("Dude", "it", mapOf<String, String>("memberId" to "10000", "name" to "Dude")) {
                    if (it.isSuccess) {
                        logger.info("Successfully got AccessToken3")
                        val ct3_local = it.value //This will also be returned to the outer scope (ct3), but you also have access to ct3 in here...
                    }
                    else {
                        logger.error("An error occurred in getting the AccessToken:", it.cause)
                        logger.info("Oh, and we didn't need to throw an exception (or a tantrum, for that matter)! But if exceptions are your thing...")
                        logger.info("...you can always do  <code>throw it.cause!! //This will break out of this routine...</code>")
                        logger.info("I took 'it' and threw 'it' on the GROOOOOUUUND!!! I'm not part of YOUR system!")
                    }
                }

                logger.info("Not sure if I *actually* got a accessToken, but I'm going to print whatever the result is which is = ${ct3?.substring(0, 10)}")

                var ct4 : String? = null

                logger.info("*********** Calling createIdentityToken...5 ************")

                ct4 = tokenManager4.createAccessToken("MySpyCar", "MINI", mapOf<String, String>("GoldMember" to "Faja"))

                logger.info("Another unknown accessToken, but I'm going to print whatever the result is which is = ${ct4?.substring(0, 10)}")
                logger.info("Let me give this last Access Tokem to a new instance of TokenManager. It *should* return isValid() = true...")

                val tokenManager5 = com.asanasoft.gsml.client.TokenManager()
                tokenManager5.accessToken = ct4

                logger.info("tokenManager5.isVaid() = ${tokenManager5.isValid}")

                logger.info("=================================================")
                logger.info("TokenManager Claims...")
                tokenManager.context?.forEach { logger.info("${it.key} - ${it.value}") }
                logger.info("TokenManager2 Claims...")
                tokenManager2.context?.forEach { logger.info("${it.key} - ${it.value}") }
                logger.info("TokenManager3 Claims...")
                tokenManager3.context?.forEach { logger.info("${it.key} - ${it.value}") }
                logger.info("=================================================")

                //Test kotlin serializer....
                val context : Map<String, String>?

                try {
                    val accessToken = Injector.inject(AccessToken::class.java)
                    accessToken.marshalled = ct
                    context = accessToken.getContext()
                    logger.info("Printing the ct context...")
                    logger.info(Json.encodeToString(context))
                }
                catch (e : Exception) {
                    logger.error("An error occurred unmarshalling ct", e)
                }

                var context2 : Map<String, String>? = null

                try {
                    val accessToken2 = Injector.inject(AccessToken::class.java) //alternatively (recommended), you can inject a "configured" AccessToken...
                    accessToken2.marshalled = ct4
                    context2 = accessToken2.getContext()
                }
                catch (e : Exception) {
                    logger.error("An error occurred unmarshalling ct4", e)
                }

                logger.info("*********** Creating another TokenManager to test CoR... ***********")
                val tokenManager6 = com.asanasoft.gsml.client.TokenManager()

                logger.info("Setting a faux AccessToken...")
                val refreshTokenId = UUID.randomUUID().toString()
                val fauxAccessToken = "{" +
                                       "\"ct\":\"This is my demo marshalled AccessToken. It's stoopid!\"," +
                                       "\"it\":\"This is my demo marshalled IdentityToken. It's stoopid!\"," +
                                       "\"rt\":\"This is my demo marshalled RefreshToken. It's stoopid!\"" +
                                       "\"rti\":\"" + refreshTokenId + "\"" +
                                       "}"

                tokenManager6.accessToken = fauxAccessToken
                if (tokenManager6.isValid) logger.info("AccessToken implementation found!")

                logger.info("*********** Ending testing CoR... ***********")

                logger.info("Printing the ct2 context...")
                logger.info(Json.encodeToString(context2))
                logger.info("=================================================")

                logger.info("Create tokenManager to accept an encrypted token...")
                val tokenManager7 = com.asanasoft.gsml.client.TokenManager()

                val ct7 = tokenManager4.createAccessToken("MySpyCar", "MINI", mapOf<String, String>("LicencePlate" to "GR8SHG"), true)

                logger.info("Encrypted accessTokenCreated...:${ct7?.substring(0, 10)}")
                logger.info("     Feeding it to tokenManager7...")

                tokenManager7.accessToken = ct7

                tokenArray.add(tokenManager)
                tokenArray.add(tokenManager2)
                tokenArray.add(tokenManager3)
                tokenArray.add(tokenManager4)
                tokenArray.add(tokenManager5)
                tokenArray.add(tokenManager6)
                tokenArray.add(tokenManager7)

                logger.info(if (tokenManager.isValid) "true" else "false")
            }
            catch (e : Exception) {
                logger.info(e.stackTraceToString())
            }
        }
    }

    suspend fun testAccessTokenCache() {
        var tokenManager = com.asanasoft.gsml.client.TokenManager()

        //call createAccessToken() again, to see if I get the same non-expired token...
        var isSame = tokenArray.last().getLastAccessToken().equals(oldAccessToken)
        logger.info("isSame = ${isSame}")

        if (isSame) {
            logger.info("AccessToken is the same. Testing validity...")
            tokenManager.accessToken = oldAccessToken
            logger.info("TokenManager.isValid = ${tokenManager.isValid}")
        }

        /**
         * AccessTokens expire after 60 secs but validation takes into consideration a "skew time", which is currently
         * set to 5 sec...
         */
        logger.info("Delaying 66sec...")
        delay(66000)

        tokenManager.accessToken = oldAccessToken
        var isExpired = !tokenManager.isValid

        logger.info("Getting last AccessToken again...")
        isSame = tokenArray.last().getLastAccessToken().equals(oldAccessToken)
        logger.info("isSame = ${isSame}")
        logger.info("isExpired = ${isExpired}")
    }

    @Test
    fun testExpiredAccessTokenRefresh() {
        logger.info("Reconstituting TokenManager...")
        var reconstitutedTokenManager = com.asanasoft.gsml.client.TokenManager()

        reconstitutedTokenManager.reconstitute(tokenArray.last().id!!, oldAccessToken!!)

        logger.info("TokenManager isInvalidState = ${reconstitutedTokenManager.isValid}")

        printContext(reconstitutedTokenManager)
    }

    private fun handler(result : Result<String>) {
        if (result.isSuccess) {
            logger.info("Success: $result.value")
        }
        else {
            logger.info("Failure!! ")
            result.cause!!.printStackTrace()
        }
    }

    suspend fun testMutex1() {
        mucinex.withLock {
            logger.debug("Executing testMutex1")
            someList.forEach { it.eventTriggered(Success("SomeMessage")) }
        }
    }

    suspend fun testMutex2() {
        mucinex.withLock {
            logger.debug("Executing testMutex2")
            someList.clear()
        }
    }

    suspend fun testNoMutex1() {
        logger.debug("Executing testNoMutex1")
        someList.forEach { it.eventTriggered(Success("SomeMessage")) }
    }

    suspend fun testNoMutex2() {
        logger.debug("Executing testNoMutex2")
        someList.clear()
    }

    @OptIn(ExperimentalTime::class)
    suspend fun waitAround() {
        var random : Long
        var targetNumber : Long = Math.round(Math.random() * RANDOM_MAX_NUMBER) + 1
        logger.info("Starting process with target = $targetNumber ...")
        logger.info("=================================================")

        var timeSource = TestTimeSource()
        var timeMark : TimeMark = timeSource.markNow()
        timeSource += 6.minutes //6 minutes from now...

        try {
            var done = false
            while (tokenArray.find {
                    logger.debug { "Testing isValid...$it" }
                    it.isValid
                } != null && !done) {
                delay(1000)
                random = Math.round(Math.random() * RANDOM_MAX_NUMBER) + 1

                logger.info("Random number = $random ...")

                if ((random.equals(targetNumber)) || Instant.now().isAfter(startTime.plusSeconds(600))) { //don't go over two minutes
                    logger.info("=================================================")
                    logger.info("Invalidating token managers with...")
                    done = true
                }
            }
        }
        catch (e : Exception) {
            logger.error(e) { "An error occurred in waitAround:" }
        }
        finally {
            tokenArray.forEach {
                it.invalidate {
                    if (it.isSuccess) {
                        logger.info(it.value)
                    }
                    else {
                        TokenManagerException(it.cause?.message!!, null) //Let the ErrorObject log
                    }
                }
            }
            BroadcastListenerHub.flush()
        }

        logger.info("Delaying for 30 secs to see if the application stops on its own...")
        delay(30000)
        logger.info("Delay finished. Stopping app...")
        val elapsedTime = Instant.now().epochSecond - startTime.epochSecond

        logger.info("Elapsed time = $elapsedTime sec")
    }
}

fun printContext(tokenManager : com.asanasoft.gsml.client.TokenManager) {
    logger.info("Printing context for tokenManager = ${tokenManager.id}")

    for (key in tokenManager.context?.keys!!) {
        logger.info(key + " = " + tokenManager.getContextValue(key))
    }
}

suspend fun main() {
    try {
        var tokenManagerTest = TokenManagerRegressionTest()

        tokenManagerTest.RANDOM_MAX_NUMBER = 1000

//    val tokenManager = tokenManagerTest.testCreateIdentityTokenWithNonce(null)
//    tokenManagerTest.tokenArray.add(tokenManager)

//    tokenManagerTest.fullTest()

//    logger.info("*************************************************************************")
//    logger.info("Testing signed tokens and invalidting them....")
//    tokenManagerTest.testSignedToken()
//    tokenManagerTest.testInvalidateToken()

        tokenManagerTest.tokenArray.clear()

//    logger.info("*************************************************************************")
//    logger.info("Testing encrypted tokens and invalidting them....")
//    tokenManagerTest.testEncryptedToken()
//    tokenManagerTest.testInvalidateToken()
//
//    tokenManagerTest.tokenArray.discard()

//    logger.info("*************************************************************************")
//    logger.info("Testing encrypted tokens, cache, reconstitute....")
//    tokenManagerTest.testEncryptedToken()
//    tokenManagerTest.testAccessTokenCache()
//    tokenManagerTest.testExpiredAccessTokenRefresh()

//    tokenManagerTest.testSendMessage()

    tokenManagerTest.testCreateToken()
    tokenManagerTest.waitAround()

//        runBlocking {
//            tokenManagerTest.someList.add(TokenManagerListener2())
//            tokenManagerTest.someList.add(TokenManagerListener2())
//            tokenManagerTest.someList.add(TokenManagerListener2())
//            tokenManagerTest.someList.add(TokenManagerListener2())
//
//            logger.info("Mutexing")
//
//            var job1 = launch {
//                tokenManagerTest.testMutex1()
//            }
//
//            var job2 = launch {
//                tokenManagerTest.testMutex2()
//            }
//
//            var job3 = launch {
//                tokenManagerTest.testNoMutex1()
//            }
//
//            var job4 = launch {
//                tokenManagerTest.testNoMutex2()
//            }
//
//            joinAll(job1, job2, job3, job4)
//
//            logger.info("Done mutexing")
//        }


        logger.info("End of Line")
    } catch (e: Exception) {
        logger.error("An error occurred in regression:", e)
    }
}

