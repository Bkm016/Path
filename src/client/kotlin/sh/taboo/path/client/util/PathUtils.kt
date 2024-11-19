package sh.taboo.path.client.util

import net.minecraft.client.MinecraftClient
import net.minecraft.particle.DustParticleEffect
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import kotlin.math.abs

/**
 * 路径记录和回放的工具类
 */
object PathUtils {
  
    /**
     * 在两点之间进行线性插值
     * @param start 起始点
     * @param end 结束点
     * @param progress 插值进度 (0.0 到 1.0)
     * @return 插值后的位置
     */
    fun interpolatePosition(start: Vec3d, end: Vec3d, progress: Double): Vec3d {
        return Vec3d(
            start.x + (end.x - start.x) * progress,
            start.y + (end.y - start.y) * progress,
            start.z + (end.z - start.z) * progress
        )
    }

    /**
     * 在两个角度之间进行插值，处理跨越 360/0 度的情况
     * @param start 起始角度
     * @param end 结束角度
     * @param progress 插值进度 (0.0 到 1.0)
     * @return 插值后的角度
     */
    fun interpolateAngle(start: Float, end: Float, progress: Double): Float {
        var delta = end - start
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360
        return (start + delta * progress).toFloat()
    }

    /**
     * 绘制粒子线条
     * @param world 游戏世界
     * @param start 起始点
     * @param end 结束点
     * @param color RGB颜色值
     * @param scale 粒子大小
     * @param steps 插值步数
     * @param yOffset Y轴偏移量
     */
    fun drawParticleLine(
        world: net.minecraft.world.World,
        start: Vec3d,
        end: Vec3d,
        color: Int,
        scale: Float,
        steps: Int,
        yOffset: Double = 0.1,
    ) {
        for (step in 0..steps) {
            val t = step.toFloat() / steps
            val x = start.x + (end.x - start.x) * t
            val y = start.y + (end.y - start.y) * t + yOffset
            val z = start.z + (end.z - start.z) * t

            world.addParticle(
                DustParticleEffect(color, scale),
                x, y, z,
                0.0, 0.0, 0.0
            )
        }
    }

    /**
     * 向玩家显示消息
     * @param client Minecraft客户端实例
     * @param message 要显示的消息
     */
    fun showMessage(client: MinecraftClient, message: Text) {
        client.player?.sendMessage(message, false)
    }

    /**
     * 播放音效
     * @param client Minecraft客户端实例
     * @param soundEvent 要播放的音效
     * @param pitch 音高
     */
    fun playSound(client: MinecraftClient, soundEvent: SoundEvent, pitch: Float) {
        client.player?.let { player ->
            client.world?.playSound(
                player,
                player.blockPos,
                soundEvent,
                SoundCategory.PLAYERS,
                1.0f,
                pitch
            )
        }
    }

    /**
     * 检查两点之间的距离是否在指定范围内
     * @param point1 第一个点
     * @param point2 第二个点
     * @param maxDistance 最大距离
     * @return 是否在范围内
     */
    fun isWithinRange(point1: Vec3d, point2: Vec3d, maxDistance: Double): Boolean {
        return point1.distanceTo(point2) <= maxDistance
    }

    /**
     * 检查两点之间是否有显著移动
     * @param current 当前位置
     * @param last 上一个位置
     * @param threshold 移动阈值
     * @return 是否有显著移动
     */
    fun hasSignificantMovement(current: Vec3d, last: Vec3d, threshold: Double): Boolean {
        return abs(current.x - last.x) > threshold ||
                abs(current.y - last.y) > threshold ||
                abs(current.z - last.z) > threshold
    }
} 