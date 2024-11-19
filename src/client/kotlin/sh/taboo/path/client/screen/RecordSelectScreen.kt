package sh.taboo.path.client.screen

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import sh.taboo.path.client.PathRecorder
import sh.taboo.path.client.data.PathRecord
import sh.taboo.path.client.manager.PathRecordManager
import java.awt.Desktop
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.min

/**
 * 影像选择界面
 */
class RecordSelectScreen(
    private val parent: Screen?,
    private val pathRecorder: PathRecorder,
) : Screen(Text.translatable("screen.path.select.title")) {

    private var records = PathRecordManager.loadAllRecords()
    private val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    private var scrollOffset = 0
    private val itemHeight = 25
    private val maxVisibleItems get() = (height - 120) / itemHeight
    private var confirmDeleteRecord: PathRecord? = null

    override fun init() {
        // 添加功能按钮
        addDrawableChild(
            ButtonWidget.builder(Text.translatable("screen.path.select.reload")) { button ->
                records = PathRecordManager.loadAllRecords()
                scrollOffset = 0
                clearAndInit()
            }
                .dimensions(width / 2 - 154, height - 30, 100, 20)
                .build()
        )

        addDrawableChild(
            ButtonWidget.builder(Text.translatable("screen.path.select.open_folder")) { button ->
                val recordsDir = PathRecordManager.getRecordsDir()
                val os = System.getProperty("os.name").toLowerCase()
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("explorer.exe /select,${recordsDir.absolutePath}")
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec("open ${recordsDir.absolutePath}")
                } else {
                    button.message = Text.translatable("screen.path.select.open_folder.error")
                }
            }
                .dimensions(width / 2 - 50, height - 30, 100, 20)
                .build()
        )

        addDrawableChild(
            ButtonWidget.builder(Text.translatable("gui.back")) { button ->
                client?.setScreen(parent)
            }
                .dimensions(width / 2 + 54, height - 30, 100, 20)
                .build()
        )

        // 添加影像列表
        val startIndex = scrollOffset
        val endIndex = min(startIndex + maxVisibleItems, records.size)

        for (i in startIndex until endIndex) {
            val record = records[i]
            val y = height / 4 + (i - startIndex) * itemHeight

            // 影像按钮
            addDrawableChild(
                ButtonWidget.builder(
                    Text.literal("${record.name} §7(${dateFormatter.format(record.createdAt.atZone(ZoneId.systemDefault()).toLocalDateTime())})")
                ) { button ->
                    pathRecorder.startReplay(record, hasShiftDown())
                    client?.setScreen(null)
                }
                    .dimensions(width / 2 - 100, y, 175, 20)
                    .build()
            )

            // 删除按钮
            addDrawableChild(
                ButtonWidget.builder(Text.literal("×")) { button ->
                    confirmDeleteRecord = record
                    clearAndInit()
                }
                    .dimensions(width / 2 + 80, y, 20, 20)
                    .build()
            )
        }

        // 如果有待确认删除的记录，显示确认对话框
        confirmDeleteRecord?.let { record ->
            addDrawableChild(
                ButtonWidget.builder(Text.translatable("screen.path.select.delete.confirm")) { button ->
                    PathRecordManager.deleteRecord(record.name)
                    records = PathRecordManager.loadAllRecords()
                    confirmDeleteRecord = null
                    clearAndInit()
                }
                    .dimensions(width / 2 - 100, height / 2, 95, 20)
                    .build()
            )

            addDrawableChild(
                ButtonWidget.builder(Text.translatable("screen.path.select.delete.cancel")) { button ->
                    confirmDeleteRecord = null
                    clearAndInit()
                }
                    .dimensions(width / 2 + 5, height / 2, 95, 20)
                    .build()
            )
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)

        // 绘制标题
        context.drawCenteredTextWithShadow(
            textRenderer,
            title,
            width / 2,
            20,
            0xFFFFFF
        )

        // 如果有待确认删除的记录，显示确认消息
        confirmDeleteRecord?.let { record ->
            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("screen.path.select.delete.message", record.name),
                width / 2,
                height / 2 - 20,
                0xFF5555
            )
        }

        super.render(context, mouseX, mouseY, delta)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double,
    ): Boolean {
        if (records.size > maxVisibleItems) {
            scrollOffset = (scrollOffset - verticalAmount.toInt())
                .coerceIn(0, records.size - maxVisibleItems)
            clearAndInit()
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }
} 