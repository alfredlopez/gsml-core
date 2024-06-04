package com.asanasoft.gsml.client.token

import com.asanasoft.gsml.exception.TokenManagerException

class InvalidPublicKeyException(override val message : String) : TokenManagerException(message)