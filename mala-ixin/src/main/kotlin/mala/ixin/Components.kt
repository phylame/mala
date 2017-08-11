package mala.ixin

import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

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

interface ActionToast {
    fun toast(text: String)

    fun reset()
}

fun <T : Component> T.toast(toast: ActionToast, action: Action): T = toast(toast) {
    action[Action.LONG_DESCRIPTION] ?: ""
}

fun <T : Component> T.toast(toast: ActionToast, text: String): T = toast(toast) { text }

fun <T : Component> T.toast(toast: ActionToast, text: () -> String): T {
    if (this is JComponent) {
        toolTipText = null
    }
    addMouseListener(ToastSupport(text, toast))
    return this
}

private class ToastSupport(val supplier: () -> String, val toast: ActionToast) : MouseAdapter() {
    private var closed = true

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
            toast.toast(text)
            closed = false
        }
    }

    private fun closeToast() {
        if (!closed) {
            toast.reset()
            closed = true
        }
    }
}