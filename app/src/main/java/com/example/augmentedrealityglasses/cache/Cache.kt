package com.example.augmentedrealityglasses.cache

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Generic contract for a key–value cache where:
 *  - Keys are always Strings
 *  - Values are serialized/deserialized using Kotlinx Serialization
 *  - Each value is stored together with metadata (CacheEntry) containing the saved timestamp
 */
interface Cache {
    // JSON instance used for serialization/deserialization
    val json: Json

    /**
     * Stores a value in the cache under the given string key.
     *
     * @param key         The element key (String).
     * @param value       The value to store.
     * @param serializer  Serializer for the type T (needed by Kotlinx Serialization).
     * @param timeProvider Time provider to record the save timestamp (for expiration policies).
     */
    suspend fun <T> set(
        key: String,
        value: T,
        serializer: KSerializer<T>,
        timeProvider: TimeProvider
    )

    /**
     * Retrieves the cache entry metadata (CacheEntry) for a key,
     * without deserializing the actual value.
     *
     * @param key The cache key.
     * @return The CacheEntry if present, null otherwise.
     */
    suspend fun getEntry(key: String): CacheEntry?

    /**
     * Retrieves and deserializes the payload (value) for the given key,
     * ignoring any freshness/expiration policy.
     *
     * @param key         The cache key.
     * @param serializer  Serializer for the type T.
     * @return The cached value if found, null otherwise.
     */
    suspend fun <T> getPayload(
        key: String,
        serializer: KSerializer<T>
    ): T?

    /**
     * Retrieves and deserializes the value for the given key
     * only if the associated CacheEntry is considered "fresh"
     * according to the provided CachePolicy.
     *
     * @param key         The cache key.
     * @param policy      The freshness/expiration policy to check.
     * @param serializer  Serializer for the type T.
     * @param timeProvider Time provider to compare timestamps.
     * @return The cached value if valid, null otherwise.
     */
    suspend fun <T> getIfValid(
        key: String,
        policy: CachePolicy,
        serializer: KSerializer<T>,
        timeProvider: TimeProvider
    ): T? {
        val entry = getEntry(key) ?: return null
        if (!policy.isFresh(entry, timeProvider)) return null
        return getPayload(key, serializer)
    }

    /**
     * Removes the entry for the given key from the cache.
     */
    suspend fun clear(key: String)

    /**
     * Removes all entries from the cache.
     */
    suspend fun clearAll()
}