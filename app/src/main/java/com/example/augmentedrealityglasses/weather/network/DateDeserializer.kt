package com.example.augmentedrealityglasses.weather.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.util.Date

class DateDeserializer : JsonDeserializer<Date> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Date {
        //unix time is in seconds
        val timestampSec = json.asLong
        return Date(timestampSec * 1000)
    }
}