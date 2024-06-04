package com.asanasoft.gsml.client.events

/**
 * Types used to identify events. These are categorizations of events.
 * Specific events are thrown based on these categories.
 *
 * @constructor Create empty Event type
 */
enum class EventType {
    REVOKE,
    WAKE,
    REFRESH,
    EVENT,
    NO_TOKEN,
    INVALID,
    ERROR,
    REGISTER,
    UNREGISTER,
    SUCCESS
}