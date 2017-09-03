package mala.ixin

import jclp.util.StringUtils
import java.awt.Component
import java.awt.Container
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.text.JTextComponent

fun Component.toggleVisible(): Boolean {
    val visible = isVisible
    isVisible = !visible
    return !visible
}

operator fun Container.plusAssign(comp: Component) {
    add(comp)
}

val JTextComponent.selectionLength get() = Math.abs(selectionStart - selectionEnd)

fun JTextComponent.toggleCase() {
    val end = selectionEnd
    val start = selectionStart
    val position = caretPosition
    if (end != start) {
        val str = selectedText
        replaceSelection(if (StringUtils.isUpperCase(str)) {
            str.toLowerCase()
        } else {
            str.toUpperCase()
        })
        caretPosition = end
        moveCaretPosition(position)
    } else {
        val str = text
        text = if (StringUtils.isUpperCase(str)) {
            str.toLowerCase()
        } else {
            str.toUpperCase()
        }
        caretPosition = position
    }
}

val JTextArea.column get() = caretPosition - getLineStartOffset(row)

val JTextArea.row get() = getLineOfOffset(caretPosition)

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

class TabEvent(source: Any, val component: Component) : EventObject(source)

interface TabListener : EventListener {
    fun tabCreated(e: TabEvent) {}

    fun tabActivated(e: TabEvent) {}

    fun tabInactivated(e: TabEvent) {}

    fun tabClosed(e: TabEvent) {}
}

interface ITab {
    val titleTip: String?

    val titleBar: Component?
}

open class ITabbedPane : JTabbedPane() {
    fun addTabListener(l: TabListener) {
        listenerList.add(TabListener::class.java, l)
    }

    fun removeTabListener(l: TabListener) {
        listenerList.remove(TabListener::class.java, l)
    }

    val tabListeners get() = listenerList.getListeners(TabListener::class.java)

    @Command
    fun nextTab() {
        val count = tabCount
        var index = selectedIndex

        if (count < 2 || index == -1) {
            return
        }

        if (index == count - 1) {
            index = 0
        } else {
            ++index
        }

        selectedIndex = index
    }

    @Command
    fun previousTab() {
        val count = tabCount
        var index = selectedIndex

        if (count < 2 || index == -1) {
            return
        }

        if (index == 0) {
            index = count - 1
        } else {
            --index
        }

        selectedIndex = index
    }

    override fun insertTab(title: String?, icon: Icon?, component: Component, tip: String?, index: Int) {
        super.insertTab(title, icon, component, tip, index)
        if (component is ITab) {
            setTabComponentAt(index, component.titleBar)
            setToolTipTextAt(index, component.titleTip)
        }
        fireTabEvent(component, TabListener::tabCreated)
    }

    override fun setSelectedIndex(index: Int) {
        val currentIndex = selectedIndex
        val currentComponent = selectedComponent
        super.setSelectedIndex(index)
        if (currentIndex == -1) {
            return
        }
        if (currentIndex != index) {
            fireTabEvent(currentComponent, TabListener::tabInactivated)
        }
        fireTabEvent(selectedComponent, TabListener::tabActivated)
    }

    override fun removeTabAt(index: Int) {
        val component = selectedComponent
        super.removeTabAt(index)
        fireTabEvent(component, TabListener::tabClosed)
    }

    private fun fireTabEvent(component: Component, action: TabListener.(TabEvent) -> Unit) {
        listenerList.fireEvent(TabListener::class.java, TabEvent(this, component), action)
    }
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
