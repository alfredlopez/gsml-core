package com.asanasoft.gsml.server

interface HttpServerProxy : ServerProxy {
    var identityTokenEndPoint : String
    var revokedTokensEndPoint : String
    var rotatedKeysEndPoint : String
    var refreshIdentityTokenEndPoint : String
    var accessTokenEndPoint : String
}