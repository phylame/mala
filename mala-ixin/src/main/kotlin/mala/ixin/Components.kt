package mala.ixin

import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

fun Component.toggleVisible(): Boolean {
    val visible = isVisible
    isVisible = !visible
    return !visible
}

fun AbstractButton.update(action: Action) {
    pressedIcon = action[IAction.PRESSED_ICON_KEY]
    selectedIcon = action[IAction.SELECTED_ICON_KEY]
    rolloverIcon = action[IAction.ROLLOVER_ICON_KEY]
    rolloverSelectedIcon = action[IAction.ROLLOVER_SELECTED_ICON_KEY]
    disabledIcon = action[IAction.DISABLED_ICON_KEY]
    disabledSelectedIcon = action[IAction.DISABLED_SELECTED_ICON_KEY]
}

fun <T : JToolBar> T.attach(button: AbstractButton): T {
    if (button.icon != null) {
        button.hideActionText = true
    }
    val action = button.action
    if (action != null) {
        var tip: String? = action[Action.SHORT_DESCRIPTION]
        if (tip != null && tip.isNotEmpty()) {
            val keyStroke: KeyStroke? = action[Action.ACCELERATOR_KEY]
            if (keyStroke != null) {
                tip += " (${keyStroke.name})"
            }
            button.toolTipText = tip
        }
    }
    button.isFocusable = false
    button.horizontalTextPosition = JButton.CENTER
    button.verticalTextPosition = JButton.BOTTOM
    add(button)
    return this
}

interface ComponentInspector {
    fun inspect(text: String)

    fun reset()
}

fun <T : Component> T.inspect(inspector: ComponentInspector, action: Action): T = inspect(inspector) {
    action[Action.LONG_DESCRIPTION] ?: ""
}

fun <T : Component> T.inspect(inspector: ComponentInspector, text: String): T = inspect(inspector) {
    text
}

fun <T : Component> T.inspect(inspector: ComponentInspector, text: () -> String): T {
    if (this is JComponent) {
        toolTipText = null
    }
    addMouseListener(InspectorSupport(text, inspector))
    return this
}

private class InspectorSupport(val supplier: () -> String, val inspector: ComponentInspector) : MouseAdapter() {
    private var isClosed = true

    override fun mouseEntered(e: MouseEvent) {
        showToast()
    }

    override fun mouseExited(e: MouseEvent) {
        closeToast()
    }

    override fun mouseReleased(e: MouseEvent) {
        closeToast()
    }

    private fun showToast() {
        val text = supplier()
        if (text.isNotEmpty()) {
            inspector.inspect(text)
            isClosed = false
        }
    }

    private fun closeToast() {
        if (!isClosed) {
            inspector.reset()
            isClosed = true
        }
    }
}
