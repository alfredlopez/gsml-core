package com.asanasoft.gsml.client.token

open class ValidatedAccessToken(delegate : AccessToken) : AccessToken by delegate {
    override fun validate() : Boolean {
        return true
    }
}