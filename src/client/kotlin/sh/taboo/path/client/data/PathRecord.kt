package sh.taboo.path.client.data

import net.minecraft.util.math.Vec3d
import java.time.Instant

/**
 * 路径影像记录
 */
data class PathRecord(
    val name: String,
    val createdAt: Instant,
    val points: List<TimelinePoint>
) {
    data class TimelinePoint(
        val position: Vec3d,
        val yaw: Float,
        val pitch: Float,
        val timestamp: Long,
        val velocity: Vec3d
    )
} 