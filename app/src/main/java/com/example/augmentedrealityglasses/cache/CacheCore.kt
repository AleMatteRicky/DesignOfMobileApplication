package com.example.augmentedrealityglasses.cache

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents a single cache record.
 *
 * @property savedAt  The timestamp (in milliseconds) when the value was saved.
 * @property payload  The actual cached data, stored as a JsonElement so it can be deserialized
 *                    into any type later.
 */
@Serializable
data class CacheEntry(
    val savedAt: Long,
    val payload: JsonElement
)

/**
 * Defines a strategy for deciding whether a cache entry is still "fresh"
 * (valid to use) or should be considered expired/not valid.
 */
interface CachePolicy {
    /**
     * @param entry         The cache entry to check.
     * @param timeProvider  Provides the current time for comparison.
     * @return true if the entry is still fresh, false if it should be refreshed.
     */
    fun isFresh(entry: CacheEntry, timeProvider: TimeProvider): Boolean
}

/**
 * Cache policy that considers entries fresh only if they are younger than a given maximum age.
 *
 * @param maxAgeMillis  The maximum allowed age in milliseconds.
 */
class MaxAgePolicy(private val maxAgeMillis: Long) : CachePolicy {
    override fun isFresh(entry: CacheEntry, timeProvider: TimeProvider) =
        (timeProvider.now() - entry.savedAt) <= maxAgeMillis
}

/**
 * Cache policy that never expires — entries are always considered fresh.
 */
object NeverExpires : CachePolicy {
    override fun isFresh(entry: CacheEntry, timeProvider: TimeProvider) = true
}

/**
 * Cache policy that always forces a refresh — entries are never considered fresh.
 */
object AlwaysRefresh : CachePolicy {
    override fun isFresh(entry: CacheEntry, timeProvider: TimeProvider) = false
}