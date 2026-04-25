package com.denko.flowerclassifier

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Classification(
    val id: Long,
    val timestamp: Long,
    val topClass: String,
    val confidence: Float,
    val imagePath: String
)

object ClassificationStore {
    private const val FILE_NAME = "history.json"

    private val _entries = MutableStateFlow<List<Classification>>(emptyList())
    val entries: StateFlow<List<Classification>> = _entries.asStateFlow()

    @Volatile private var initialized = false

    suspend fun init(context: Context) {
        if (initialized) return
        initialized = true
        load(context)
    }

    private suspend fun load(context: Context) = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return@withContext
        try {
            val json = JSONArray(file.readText())
            val list = mutableListOf<Classification>()
            for (i in 0 until json.length()) {
                val o = json.getJSONObject(i)
                list.add(
                    Classification(
                        id = o.getLong("id"),
                        timestamp = o.getLong("timestamp"),
                        topClass = o.getString("topClass"),
                        confidence = o.getDouble("confidence").toFloat(),
                        imagePath = o.getString("imagePath")
                    )
                )
            }
            _entries.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun save(context: Context) = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        _entries.value.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("timestamp", e.timestamp)
                put("topClass", e.topClass)
                put("confidence", e.confidence.toDouble())
                put("imagePath", e.imagePath)
            })
        }
        File(context.filesDir, FILE_NAME).writeText(arr.toString())
    }

    suspend fun add(
        context: Context,
        topClass: String,
        confidence: Float,
        imagePath: String
    ) {
        val now = System.currentTimeMillis()
        val entry = Classification(
            id = now,
            timestamp = now,
            topClass = topClass,
            confidence = confidence,
            imagePath = imagePath
        )
        _entries.value = listOf(entry) + _entries.value
        save(context)
    }

    suspend fun deleteById(context: Context, id: Long) {
        _entries.value = _entries.value.filter { it.id != id }
        save(context)
    }

    suspend fun deleteAll(context: Context) {
        _entries.value.forEach { File(it.imagePath).delete() }
        _entries.value = emptyList()
        save(context)
    }
}