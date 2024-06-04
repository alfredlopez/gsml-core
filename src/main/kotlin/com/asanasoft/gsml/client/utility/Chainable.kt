package com.asanasoft.gsml.client.utility

interface Chainable<T> {
    var previous : T?
    var next : T?

    fun discard(other: T?){
        val _prev = previous?.takeIf {
            (it is Chainable<*>)
        }
        val _next = this.next?.takeIf {
            (it is Chainable<*>)
        }

        _prev?.let {
            with (_prev as Chainable<T>) {
                if (_prev != other) this.discard(_prev)
            }

            this.previous = null
        }

        _next?.let {
            with (_next as Chainable<T>) {
                if (_next != other) this.discard(_next)
            }

            this.next = null
        }
    }
}