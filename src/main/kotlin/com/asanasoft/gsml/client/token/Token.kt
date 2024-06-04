package com.asanasoft.gsml.client.token

import com.asanasoft.gsml.client.utility.Chainable
import java.util.*

interface Token : Chainable<Token> {
    val type : TokenType
    var marshalled : String?
    var principal : String
    var nonce : String
    var payload : String
    var issuedAt : Date
    var expiresAt : Date
    var isValid : Boolean
    var encrypted : Boolean //Signifies that this is an "encrypted (noun) token" instead of a "token that is encrypted (verb)"

    fun validate() : Boolean
    fun fromMarshalled(marshalled : String) : Token?
    fun populateContext(context : MutableMap<String, String>)

    fun discard() {
        previous?.discard(this)
        next?.discard(this)

        marshalled  = null
    }
}