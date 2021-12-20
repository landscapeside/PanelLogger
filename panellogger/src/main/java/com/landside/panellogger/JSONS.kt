package com.landside.panellogger

import android.text.TextUtils
import com.google.gson.*
import java.lang.reflect.Type

internal object JSONS {
    private var sGson: Gson = GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create()
    private val sParser: JsonParser = JsonParser()

    fun <T> parseObject(json: String?, type: Type?): T? {
        if (TextUtils.isEmpty(json)) {
            return null
        }
        try {
            return sGson.fromJson(json, type)
        } catch (e: Exception) {
        }
        return null
    }

    fun parseJson(o: Any?): String? {
        return if (o == null) {
            null
        } else try {
            sGson.toJson(o)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * convert string to JsonObject
     *
     * @param content
     * @return
     */
    fun parseJsonObject(content: String?): JsonObject? {
        return if (TextUtils.isEmpty(content)) {
            null
        } else sParser.parse(content).getAsJsonObject()
    }

    fun parseJsonArray(content: String?): JsonArray? {
        return if (TextUtils.isEmpty(content)) {
            null
        } else sParser.parse(content).getAsJsonArray()
    }

    fun String.isJson(): Boolean {
        if (isEmpty()) {
            return false
        }
        return try {
            JsonParser().parse(this)
            true
        } catch (e: JsonParseException) {
            false
        }
    }
}