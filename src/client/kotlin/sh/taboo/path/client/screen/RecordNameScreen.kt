package sh.taboo.path.client.screen

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text
import sh.taboo.path.client.PathRecorder
import sh.taboo.path.client.manager.PathRecordManager

/**
 * 录制名称输入界面
 */
class RecordNameScreen(
    private val parent: Screen?,
    private val pathRecorder: PathRecorder,
) : Screen(Text.translatable("screen.path.record.title")) {
    private lateinit var nameField: TextFieldWidget
    private lateinit var startButton: ButtonWidget
    private var errorMessage: String? = null

    override fun init() {
        nameField = TextFieldWidget(
            textRenderer,
            width / 2 - 100,
            height / 4 + 20,
            200,
            20,
            Text.translatable("screen.path.record.name")
        ).apply {
            setMaxLength(32)
            setChangedListener { text ->
                errorMessage = when {
                    text.isEmpty() -> "screen.path.record.error.empty"
                    !text.matches(Regex("[a-zA-Z0-9_-]+")) -> "screen.path.record.error.invalid"
                    PathRecordManager.isNameExists(text) -> "screen.path.record.error.exists"
                    else -> null
                }
                startButton.active = errorMessage == null
            }
        }

        startButton = ButtonWidget.builder(
            Text.translatable("screen.path.record.start")
        ) { button ->
            pathRecorder.startRecording(nameField.text)
            client?.setScreen(null)
        }
            .dimensions(width / 2 - 100, height / 4 + 60, 200, 20)
            .build()

        addDrawableChild(nameField)
        addDrawableChild(startButton)

        startButton.active = false
        setInitialFocus(nameField)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        context.drawCenteredTextWithShadow(
            textRenderer,
            title,
            width / 2,
            height / 4,
            0xFFFFFF
        )

        errorMessage?.let { message ->
            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable(message),
                width / 2,
                height / 4 + 45,
                0xFF5555
            )
        }

        super.render(context, mouseX, mouseY, delta)
    }
} 