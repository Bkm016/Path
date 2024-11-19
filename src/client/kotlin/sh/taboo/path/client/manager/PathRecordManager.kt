package sh.taboo.path.client.manager

import com.google.gson.*
import net.minecraft.util.math.Vec3d
import sh.taboo.path.client.data.PathRecord
import java.io.File
import java.lang.reflect.Type
import java.time.Instant

/**
 * 路径影像管理器
 */
object PathRecordManager {
    private val gson = GsonBuilder()
        .registerTypeAdapter(Vec3d::class.java, Vec3dSerializer())
        .registerTypeAdapter(Instant::class.java, InstantSerializer())
        .setPrettyPrinting()
        .create()
    
    private val recordsDir = File("path-records").apply { mkdirs() }
    
    /**
     * 保存影像
     */
    fun saveRecord(record: PathRecord) {
        val file = File(recordsDir, "${record.name}.json")
        file.writeText(gson.toJson(record))
    }
    
    /**
     * 加载所有影像
     */
    fun loadAllRecords(): List<PathRecord> {
        return recordsDir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    gson.fromJson(file.readText(), PathRecord::class.java)
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }
    
    /**
     * 检查名称是否已存在
     */
    fun isNameExists(name: String): Boolean {
        return File(recordsDir, "$name.json").exists()
    }
    
    /**
     * Vec3d 序列化器
     */
    private class Vec3dSerializer : JsonSerializer<Vec3d>, JsonDeserializer<Vec3d> {
        override fun serialize(src: Vec3d, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                addProperty("x", src.x)
                addProperty("y", src.y)
                addProperty("z", src.z)
            }
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Vec3d {
            val obj = json.asJsonObject
            return Vec3d(
                obj.get("x").asDouble,
                obj.get("y").asDouble,
                obj.get("z").asDouble
            )
        }
    }
    
    /**
     * Instant 序列化器
     */
    private class InstantSerializer : JsonSerializer<Instant>, JsonDeserializer<Instant> {
        override fun serialize(src: Instant, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.toEpochMilli())
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Instant {
            return Instant.ofEpochMilli(json.asLong)
        }
    }
    
    /**
     * 获取影像存储目录
     */
    fun getRecordsDir(): File = recordsDir
    
    /**
     * 删除指定影像
     */
    fun deleteRecord(name: String): Boolean {
        return try {
            File(recordsDir, "$name.json").delete()
        } catch (e: Exception) {
            false
        }
    }
} 