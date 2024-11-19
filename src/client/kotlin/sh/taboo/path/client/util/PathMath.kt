package sh.taboo.path.client.util

import net.minecraft.util.math.Vec3d

/**
 * 路径相关的数学计算工具类
 */
object PathMath {
  
    /**
     * 计算路径总长度
     */
    fun calculatePathLength(points: List<Vec3d>): Double {
        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += points[i].distanceTo(points[i + 1])
        }
        return totalDistance
    }

    /**
     * 计算路径段的插值位置
     */
    fun calculateSegmentPosition(
        points: List<Vec3d>,
        progress: Double,
        totalSegments: Int
    ): Pair<Int, Double> {
        val currentSegment = (progress * totalSegments).toInt()
        val segmentProgress = (progress * totalSegments) % 1.0
        return Pair(currentSegment, segmentProgress)
    }

    /**
     * 计算移动速度
     */
    fun calculateMoveSpeed(
        currentPos: Vec3d,
        targetPos: Vec3d,
        baseSpeed: Double
    ): Vec3d {
        val direction = targetPos.subtract(currentPos)
        val distance = direction.length()
        
        return if (distance > 0) {
            direction.multiply(1.0 / distance * baseSpeed)
        } else {
            Vec3d.ZERO
        }
    }

    /**
     * 计算两点之间的瞬时速度
     * @param start 起始点
     * @param end 结束点
     * @param timeDelta 时间差（毫秒）
     * @return 每tick的移动速度（格/tick）
     */
    fun calculateInstantSpeed(start: Vec3d, end: Vec3d, timeDelta: Long): Double {
        val distance = start.distanceTo(end)
        // 将毫秒转换为tick (20 ticks/second)
        val tickDelta = timeDelta / 50.0 // 50ms per tick
        return if (tickDelta > 0) distance / tickDelta else 0.0
    }
} 