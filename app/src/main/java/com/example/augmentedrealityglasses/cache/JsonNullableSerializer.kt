package com.example.augmentedrealityglasses.cache

import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * A DataStore Serializer implementation that:
 *  - Serializes and deserializes objects of type T using Kotlinx Serialization
 *  - Allows the stored value to be nullable (T?)
 *  - Ignores unknown keys during deserialization for forward compatibility
 */
class JsonNullableSerializer<T>(
    private val kserializer: KSerializer<T> // Serializer for the specific type T
) : Serializer<T?> {

    // JSON configuration: ignoreUnknownKeys = true allows loading older cache files
    // even if the model has changed by adding new fields
    private val json = Json {
        ignoreUnknownKeys = true
    }

    // Default value if the file does not exist or is empty
    override val defaultValue: T? = null

    /**
     * Reads and deserializes the object from the InputStream.
     * If deserialization fails (e.g., due to malformed JSON), returns null.
     */
    override suspend fun readFrom(input: InputStream): T? =
        runCatching {
            json.decodeFromString(
                kserializer,
                input.readBytes().decodeToString()
            )
        }.getOrNull()

    /**
     * Serializes the given object (if not null) and writes it to the OutputStream.
     * The actual write is performed on the IO dispatcher to avoid blocking the main thread.
     */
    override suspend fun writeTo(t: T?, output: OutputStream) {
        if (t != null) withContext(Dispatchers.IO) {
            output.write(
                json.encodeToString(kserializer, t).encodeToByteArray()
            )
        }
    }
}
