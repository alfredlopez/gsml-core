package com.asanasoft.gsml.client.token

import com.asanasoft.gsml.client.noop
import com.asanasoft.gsml.exception.UnmarshallException
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

abstract class AbstractToken : Token {
    private var delegate : Token? = null
    private var _isValid : Boolean? = null
    private var _marshalled : String? = null

    private var _encrypted : Boolean = false

    override var encrypted : Boolean
        get() {
            return _encrypted
        }
        set(value) {
            _encrypted = value
        }

    override var isValid : Boolean
        get() {
            try {
                _isValid = validate()
            }
            catch (e : Exception) {
                _isValid = false
            }

            return _isValid!!
        }
        set(value) {
            noop()
        }

    override var previous : Token? = null
    override var next : Token? = null

    override lateinit var expiresAt : Date
    override lateinit var issuedAt : Date

    override var marshalled : String?
        get() {
            return _marshalled
        }
        set(value) {
            logger.debug("Marshalled Token: $value")

            value?.let {
                if (fromMarshalled(it ?: "") == null) throw UnmarshallException("No successful unmarshall implementation for current input!!")
            }

            _marshalled = value
        }

    override fun fromMarshalled(marshalled : String) : Token? {
        var result : Token? = null

        if (_marshalled == null) {
            _marshalled = marshalled
            logger.trace("Marshalled Value:\n $_marshalled")
            _isValid = null
            result = unmarshal()

            if (result == null) result = next?.fromMarshalled(marshalled)
        }

        return result
    }

    /**
     * Unmarshals this Token
     * @return this Token if unmarshal was successful, otherwise null
     */
    abstract fun unmarshal() : Token?

    override fun validate() : Boolean {
        return false
    }

    override fun discard() {
        super.discard()
        this.delegate = null
    }

    protected open fun validate(keyId : String) : Boolean {
        return false
    }
}