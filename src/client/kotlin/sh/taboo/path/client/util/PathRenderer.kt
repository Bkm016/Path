package sh.taboo.path.client.util

import net.minecraft.client.MinecraftClient
import net.minecraft.particle.DustParticleEffect
import net.minecraft.util.math.Vec3d

/**
 * 路径渲染工具类
 */
object PathRenderer {
  
    private const val MAX_PARTICLE_DISTANCE = 16.0
    private const val PARTICLE_DISPLAY_INTERVAL = 100L
    private var lastParticleTime = 0L

    /**
     * 渲染路径
     */
    fun renderPath(
        client: MinecraftClient,
        points: List<Vec3d>,
        color: Int = 0xFF0000,
        skipPoints: Int = 3
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastParticleTime < PARTICLE_DISPLAY_INTERVAL) return
        
        val world = client.world ?: return
        val player = client.player ?: return
        val playerPos = player.pos
        
        if (points.size >= 2) {
            for (i in 0 until points.size - 1 step skipPoints) {
                val current = points[i]
                val next = points[i + 1]
                
                if (!PathUtils.isWithinRange(playerPos, current, MAX_PARTICLE_DISTANCE) && 
                    !PathUtils.isWithinRange(playerPos, next, MAX_PARTICLE_DISTANCE)) {
                    continue
                }
                
                val distance = playerPos.distanceTo(current)
                val scale = (1.0 - (distance / MAX_PARTICLE_DISTANCE)).coerceIn(0.3, 1.0)
                
                PathUtils.drawParticleLine(
                    world,
                    current,
                    next,
                    color,
                    scale.toFloat(),
                    3
                )
            }
        }
        
        lastParticleTime = currentTime
    }

    /**
     * 渲染指引线
     */
    fun renderGuideLine(
        client: MinecraftClient,
        start: Vec3d,
        end: Vec3d,
        color: Int = 0xFFFF00
    ) {
        val world = client.world ?: return
        
        PathUtils.drawParticleLine(
            world,
            start,
            end,
            color,
            0.8f,
            20,
            0.5
        )
        
        // 终点标记
        world.addParticle(
            DustParticleEffect(color, 1.5f),
            end.x, end.y + 0.5, end.z,
            0.0, 0.0, 0.0
        )
    }
} 