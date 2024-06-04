package com.asanasoft.gsml.client.token.jwt.impl

import com.asanasoft.gsml.client.token.jwt.Jwk
import com.asanasoft.gsml.client.token.jwt.JwkProvider
import com.asanasoft.gsml.client.utility.Injector

object JwkProviderServer : JwkProvider {
    val delegate : JwkProvider = Injector.inject(JwkProvider::class.java)

    override fun get(kid : String) : Jwk {
        return delegate.get(kid)
    }
}
