package com.example.augmentedrealityglasses.cache

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File

/**
 * Implementation of [Cache] backed by Android DataStore, storing multiple key–value pairs
 * in a single file. Keys are Strings, and values are stored as CacheEntry objects.
 *
 * The underlying storage format is:
 *   Map<String, CacheEntry>
 *
 * This design allows multiple cache entries in one file, each with its own key and metadata.
 */
class DataStoreMapCache(
    private val file: File,
    externalScope: CoroutineScope? = null,
    // JSON instance used for serialization/deserialization.
    // Configured to ignore unknown keys by default for forward compatibility.
    override val json: Json = Json { ignoreUnknownKeys = true }
) : Cache {

    private val job = SupervisorJob()
    private val scope = externalScope ?: CoroutineScope(Dispatchers.IO + job)

    // Serializer for Map<String, CacheEntry>
    private val mapSerializer = MapSerializer(String.serializer(), CacheEntry.serializer())

    // DataStore instance holding an optional Map of String -> CacheEntry
    private val dataStore: DataStore<Map<String, CacheEntry>?> = DataStoreFactory.create(
        serializer = JsonNullableSerializer(mapSerializer),
        // If the file is corrupted, replace with an empty map
        corruptionHandler = ReplaceFileCorruptionHandler { emptyMap() },
        // Store the file in the app's private internal storage
        produceFile = { file },
        // Run DataStore operations on IO dispatcher
        scope = scope
    )

    // Mutex to ensure thread-safe read/write operations
    private val lock = Mutex()

    /**
     * Stores a value in the cache under the given key.
     * The value is serialized to JSON and wrapped in a CacheEntry with the current timestamp.
     */
    override suspend fun <T> set(
        key: String,
        value: T,
        serializer: KSerializer<T>,
        timeProvider: TimeProvider
    ): Unit = lock.withLock {
        val current = dataStore.data.firstOrNull().orEmpty().toMutableMap()
        val payload: JsonElement = json.encodeToJsonElement(serializer, value)
        current[key] = CacheEntry(
            savedAt = timeProvider.now(),
            payload = payload
        )
        dataStore.updateData { current }
    }

    /**
     * Retrieves only the CacheEntry metadata for the given key, without deserializing the value.
     */
    override suspend fun getEntry(key: String): CacheEntry? {
        val map = dataStore.data.firstOrNull() ?: return null
        return map[key]
    }

    /**
     * Retrieves and deserializes the value for the given key, ignoring any freshness policy.
     */
    override suspend fun <T> getPayload(
        key: String,
        serializer: KSerializer<T>
    ): T? {
        val entry = getEntry(key) ?: return null
        return runCatching { json.decodeFromJsonElement(serializer, entry.payload) }.getOrNull()
    }

    /**
     * Removes the entry for the given key from the cache.
     */
    override suspend fun clear(key: String): Unit = lock.withLock {
        val current = dataStore.data.firstOrNull().orEmpty().toMutableMap()
        current.remove(key)
        dataStore.updateData { current }
    }

    /**
     * Clears all entries in the cache file.
     */
    override suspend fun clearAll() {
        dataStore.updateData { emptyMap() }
    }
}