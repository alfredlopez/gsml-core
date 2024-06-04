package com.asanasoft.gsml.server

/**
 * Server proxy
 * This interface depicts the services server that this client lib
 * accesses
 * @constructor Create empty Server proxy
 * TODO: Implement this
 */
interface ServerProxy {
    /**
     * Get signatures
     * Get a list of signature keys in JSON format
     * @return
     */
    fun getSignatures(header : Map<String, String>?, body : Any?) : String

    /**
     * Get revoked tokens
     * Get a list of revoked tokens in JSON form
     * @return
     */
    fun getRevokedTokens(header : Map<String, String>?, body : Any?) : String

    /**
     * Get access token
     * Get a Access Token (used for app to app session exchange)
     * @return
     */
    fun getAccessToken(header : Map<String, String>?, body : Any?) : String

    /**
     * Get identity token
     * Get an identityToken using a principal in JSON form
     * @return
     */
    fun getIdentityToken(header : Map<String, String>?, body : Any?) : String

    /**
     * Refresh identity token
     * Get another identityToken using a RefreshToken in JSON form
     * @param
     * @return
     */
    fun refreshIdentityToken(header : Map<String, String>?, body : Any?) : String

    /**
     * Revoke token
     * Revoke a RefreshToken (and, thereby, the associated identityToken)
     * that matches <code>tokenId</code>
     * @param
     */
    fun revokeToken(header : Map<String, String>?, body : Any?)
}