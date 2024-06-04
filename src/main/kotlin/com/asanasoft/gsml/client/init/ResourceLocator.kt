package com.asanasoft.gsml.client.init

interface ResourceLocator<T> {
    fun fetchResource(name : String) : T
    fun fetchAll() : Set<T>
    fun fetchByFilter(predicate : (T) -> Boolean) : Set<T>
}
