package com.asanasoft.gsml.client.token.jwt.impl

import com.asanasoft.gsml.client.init.Environment

object TokenTypes {
    val PLAIN : String
    val SIGNED : String
    val ENCRYPTED : String

    init {
        val tokenTypesProps = Environment.getProperties("tokenTypes.properties", true)

        ENCRYPTED = tokenTypesProps?.getProperty("encrypted")!!
        PLAIN = tokenTypesProps.getProperty("plain")!!
        SIGNED = tokenTypesProps.getProperty("signed")!!
    }
}
