package sh.taboo.path.client

import net.minecraft.client.MinecraftClient
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import sh.taboo.path.client.data.PathRecord
import sh.taboo.path.client.manager.PathRecordManager
import sh.taboo.path.client.screen.RecordNameScreen
import sh.taboo.path.client.screen.RecordSelectScreen
import sh.taboo.path.client.util.PathRenderer
import sh.taboo.path.client.util.PathUtils
import java.time.Instant

/**
 * 路径记录器
 *
 * 基于时间线的路径记录和回放系统：
 * - 记录玩家的移动轨迹、视角和时间戳
 * - 按照原始录制速度回放路径
 * - 显示路径粒子效果和指引线
 */
class PathRecorder {

    // 状态控制
    private var isRecording = false
    private var isReplaying = false
    private var isLoopMode = false

    private var currentRecord: PathRecord? = null
    private val recordedPath = mutableListOf<TimelinePoint>()
    private var recordStartTime = 0L
    private var replayStartTime = 0L
    private var lastRecordTime = 0L
    private var currentRecordName: String? = null

    // 配置常量
    private val MAX_START_DISTANCE = 1.0 // 最大起点距离（格）
    private val RECORD_INTERVAL = 50L // 录制间隔（毫秒），一个游戏 tick

    // 指引线控制
    private var showGuideLine = false
    private var guideLineStartTime = 0L
    private val GUIDE_LINE_DURATION = 3000L // 指引线显示时间（毫秒）

    // 在类的顶部添加新的属性
    private var pendingReplay: (() -> Unit)? = null
    private var pendingReplayTime: Long = 0

    // 在类的顶部添加新的常量
    private val MAX_HORIZONTAL_DEVIATION = 3.5 // 最大水平偏差（格）
    private val MAX_VERTICAL_DEVIATION = 2.5   // 最大垂直偏差（格）

    /**
     * 开始录制新影像
     */
    fun startRecording(name: String) {
        if (isRecording || isReplaying) return

        currentRecordName = name
        recordedPath.clear()
        recordStartTime = System.currentTimeMillis()
        lastRecordTime = recordStartTime
        isRecording = true

        MinecraftClient.getInstance().let { client ->
            PathUtils.showMessage(client, Text.translatable("message.path.record.start"))
            PathUtils.playSound(client, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f)
        }
    }

    /**
     * 停止录制并保存影像
     */
    fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        currentRecordName?.let { name ->
            val record = PathRecord(
                name = name,
                createdAt = Instant.now(),
                points = recordedPath.map { point ->
                    PathRecord.TimelinePoint(
                        position = point.position,
                        yaw = point.yaw,
                        pitch = point.pitch,
                        timestamp = point.timestamp,
                        velocity = point.velocity
                    )
                }
            )
            PathRecordManager.saveRecord(record)
        }
        currentRecordName = null

        MinecraftClient.getInstance().let { client ->
            PathUtils.showMessage(client, Text.translatable("message.path.record.stop"))
            PathUtils.playSound(client, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 0.8f)
        }
    }

    /**
     * 开始回放指定影像
     */
    fun startReplay(record: PathRecord, loop: Boolean = false) {
        val client = MinecraftClient.getInstance()
        if (isRecording || isReplaying) {
            PathUtils.showMessage(client, Text.translatable("message.path.replay.already_in_progress"))
            PathUtils.playSound(client, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 0.8f)
            return
        }

        client.player?.let { player ->
            // 找到距离玩家最近的路径点
            val nearestPointIndex = record.points.withIndex().minByOrNull { (_, point) ->
                player.pos.distanceTo(point.position)
            }?.index ?: 0

            val nearestPoint = record.points[nearestPointIndex].position

            // 检查与最近点的距离是否在允许范围内
            if (!PathUtils.isWithinRange(player.pos, nearestPoint, MAX_START_DISTANCE)) {
                PathUtils.showMessage(
                    client,
                    Text.translatable(
                        "message.path.replay.too_far.with_pos",
                        String.format("%.2f", nearestPoint.x),
                        String.format("%.2f", nearestPoint.y),
                        String.format("%.2f", nearestPoint.z)
                    )
                )
                PathUtils.playSound(client, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.5f)
                showGuideLine = true
                guideLineStartTime = System.currentTimeMillis()
                return
            }

            currentRecord = record
            recordedPath.clear()
            // 修改：每次重新启动时都重置时间戳
            val baseTimestamp = record.points[nearestPointIndex].timestamp
            recordedPath.addAll(record.points.drop(nearestPointIndex).map { point ->
                TimelinePoint(
                    position = point.position,
                    yaw = point.yaw,
                    pitch = point.pitch,
                    timestamp = point.timestamp - baseTimestamp, // 确保时间戳从 0 开始
                    velocity = point.velocity
                )
            })

            isLoopMode = loop
            isReplaying = true
            replayStartTime = System.currentTimeMillis() // 重置开始时间
            PathUtils.showMessage(
                client,
                Text.translatable(
                    if (loop) "message.path.replay.start.loop" 
                    else "message.path.replay.start"
                )
            )
            PathUtils.playSound(client, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.0f)
        }
    }

    /**
     * 停止回放
     */
    fun stopReplay() {
        if (!isReplaying) return

        isReplaying = false
        MinecraftClient.getInstance().let { client ->
            PathUtils.showMessage(client, Text.translatable("message.path.replay.stop"))
            PathUtils.playSound(client, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 0.8f)
        }
    }

    /**
     * 切换记录状态
     */
    fun toggleRecording(client: MinecraftClient) {
        if (isRecording) {
            stopRecording()
        } else {
            try {
                client.setScreen(RecordNameScreen(null, this))
            } catch (e: Exception) {
                PathUtils.showMessage(client, Text.translatable("message.path.record.name.error"))
            }
        }
    }

    /**
     * 切换回放状态
     */
    fun toggleReplay(client: MinecraftClient) {
        if (isReplaying) {
            stopReplay()
        } else {
            try {
                client.setScreen(RecordSelectScreen(null, this))
            } catch (e: Exception) {
                PathUtils.showMessage(client, Text.translatable("message.path.record.select.error"))
            }
        }
    }

    fun handleRecording(client: MinecraftClient) {
        client.player?.let { player ->
            if (!isRecording) return

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRecordTime >= RECORD_INTERVAL) {
                recordedPath.add(
                    TimelinePoint(
                        position = player.pos,
                        yaw = player.yaw,
                        pitch = player.pitch,
                        timestamp = currentTime - recordStartTime,
                        velocity = player.velocity
                    )
                )
                lastRecordTime = currentTime
            }
        }
    }

    fun handleReplaying(client: MinecraftClient, tickDelta: Float) {
        // 检查是否有待执行的重放
        pendingReplay?.let { replay ->
            if (System.currentTimeMillis() >= pendingReplayTime) {
                replay.invoke()
                pendingReplay = null
                pendingReplayTime = 0
            }
        }

        client.player?.let { player ->
            if (isReplaying) {
                handleReplaying(client, player, tickDelta)
            }
        }
    }

    private fun handleReplaying(
        client: MinecraftClient,
        player: net.minecraft.client.network.ClientPlayerEntity,
        tickDelta: Float,
    ) {
        if (recordedPath.size < 2) {
            isReplaying = false
            PathUtils.showMessage(
                client,
                Text.translatable(
                    if (isLoopMode) "message.path.replay.complete.loop" 
                    else "message.path.replay.complete"
                )
            )
            // 如果是循环模式，则重新启动
            if (isLoopMode && currentRecord != null) {
                startReplay(currentRecord!!, true)
            }
            return
        }

        val currentTime = System.currentTimeMillis()
        val replayTime = currentTime - replayStartTime

        // 找到当前时间对应的路径段
        val currentIndex = recordedPath.indexOfLast { it.timestamp <= replayTime }
        val nextIndex = (currentIndex + 1).coerceAtMost(recordedPath.size - 1)

        if (currentIndex >= 0 && currentIndex < recordedPath.size - 1) {
            val current = recordedPath[currentIndex]
            val next = recordedPath[nextIndex]

            // 计算当前段的插值进度，加入 tickDelta 实现更平滑的插值
            val segmentProgress = if (current.timestamp == next.timestamp) {
                1.0
            } else {
                ((replayTime - current.timestamp).toDouble() / (next.timestamp - current.timestamp) + tickDelta / 20.0).coerceIn(0.0, 1.0)
            }

            // 插值计算位置和视角
            val interpolatedPos = PathUtils.interpolatePosition(
                current.position,
                next.position,
                segmentProgress
            )

            // 分别检查水平和垂直方向的偏差
            val horizontalDiff = Vec3d(
                interpolatedPos.x - player.pos.x,
                0.0,
                interpolatedPos.z - player.pos.z
            ).length()

            val verticalDiff = Math.abs(interpolatedPos.y - player.pos.y)
            if (horizontalDiff > MAX_HORIZONTAL_DEVIATION || verticalDiff > MAX_VERTICAL_DEVIATION) {
                // 立即停止回放
                isReplaying = false

                // 设置 1 秒后重新开始回放
                if (isLoopMode) {
                    pendingReplayTime = System.currentTimeMillis() + 1000
                    pendingReplay = {
                        startReplay(currentRecord!!, true)
                    }
                }
                PathUtils.showMessage(
                    client,
                    Text.translatable("message.path.replay.position_corrected")
                )
                return
            }

            player.yaw = PathUtils.interpolateAngle(current.yaw, next.yaw, segmentProgress)
            player.pitch = PathUtils.interpolateAngle(current.pitch, next.pitch, segmentProgress)

            // 使用记录的速度进行插值
            val interpolatedVelocity = PathUtils.interpolatePosition(
                current.velocity,
                next.velocity,
                segmentProgress
            )
            player.velocity = interpolatedVelocity
            player.updatePosition(interpolatedPos.x, interpolatedPos.y, interpolatedPos.z)
        } else {
            isReplaying = false
            PathUtils.showMessage(
                client,
                Text.translatable(
                    if (isLoopMode) "message.path.replay.complete.loop" 
                    else "message.path.replay.complete"
                )
            )
            // 如果是循环模式，则重新启动
            if (isLoopMode && currentRecord != null) {
                startReplay(currentRecord!!, true)
            }
        }
    }

    /**
     * 处理路径渲染
     *
     * 显示路径粒子效果和指引线
     * @param client Minecraft客户端实例
     */
    fun handleRendering(client: MinecraftClient) {
        // 渲染指引线
        if (showGuideLine && recordedPath.isNotEmpty() && System.currentTimeMillis() - guideLineStartTime <= GUIDE_LINE_DURATION) {
            client.player?.let { player ->
                PathRenderer.renderGuideLine(
                    client,
                    player.pos,
                    recordedPath.first().position
                )
            }
        } else {
            showGuideLine = false
        }

        // 渲染路径
        if (isRecording || isReplaying) {
            PathRenderer.renderPath(client, recordedPath.map { it.position })
        }
    }

    /**
     * 时间线路径点
     * @property position 位置坐标
     * @property yaw 水平角度
     * @property pitch 垂直角度
     * @property timestamp 相对时间戳（毫秒）
     * @property velocity 当前速度
     */
    data class TimelinePoint(
        val position: Vec3d,
        val yaw: Float,
        val pitch: Float,
        val timestamp: Long,
        val velocity: Vec3d,
    )
}