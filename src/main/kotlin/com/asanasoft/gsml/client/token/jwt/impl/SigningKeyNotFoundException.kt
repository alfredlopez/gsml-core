package com.asanasoft.gsml.client.token.jwt.impl

import com.asanasoft.gsml.exception.TokenManagerException

class SigningKeyNotFoundException(override val message : String) : TokenManagerException(message)