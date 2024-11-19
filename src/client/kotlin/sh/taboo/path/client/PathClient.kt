package sh.taboo.path.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW
import net.minecraft.client.MinecraftClient
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import sh.taboo.path.client.manager.PathRecordManager

class PathClient : ClientModInitializer {

    private lateinit var recordKey: KeyBinding
    private lateinit var replayKey: KeyBinding
    
    private val pathRecorder = PathRecorder()

    override fun onInitializeClient() {
        // 注册按键绑定
        recordKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.path.record",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.path.category"
            )
        )

        replayKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.path.replay",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "key.path.category"
            )
        )

        // 注册按键事件处理和录制
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (recordKey.wasPressed()) {
                pathRecorder.toggleRecording(client)
            }
            if (replayKey.wasPressed()) {
                pathRecorder.toggleReplay(client)
            }
            // 只在 tick 时处理录制
            pathRecorder.handleRecording(client)
            pathRecorder.handleRendering(client)
        }

        // 注册渲染事件，用于更流畅的回放和粒子渲染
        WorldRenderEvents.START.register { context: WorldRenderContext ->
            pathRecorder.handleReplaying(MinecraftClient.getInstance(), context.tickCounter().getTickDelta(true))
        }
    }
} 