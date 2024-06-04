package com.asanasoft.gsml.client.token.jwt.impl

import com.asanasoft.gsml.client.noop
import com.asanasoft.gsml.client.token.RefreshToken
import com.asanasoft.gsml.client.token.Token
import com.asanasoft.gsml.client.token.TokenType

open class JwtRefreshToken : JweToken(), RefreshToken {
    override val type = TokenType.REFRESH
    final override fun unmarshal() : Token? {
        /**
         * Don't unmarshal this token since we cannot
         * verify it's signature because on the server
         * knows how to decrypt it. We just need to keep
         * the marshalled representation.
         */
        noop()
        return this
    }

    final override fun validate() : Boolean {
        return false
    }
}