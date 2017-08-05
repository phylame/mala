package mala.ixin

import jclp.setting.Settings
import java.awt.BorderLayout
import javax.swing.*

class IStatusBar : JPanel(BorderLayout()) {
    companion object {
        var borderSize = 2
    }

    val label: JLabel = JLabel()

    var text: String get() = label.text
        set (value) {
            previous = value
            label.text = value
        }

    init {
        label.border = BorderFactory.createEmptyBorder(0, borderSize, 0, 0)
        add(JSeparator(), BorderLayout.PAGE_START)
        add(label, BorderLayout.LINE_START)
    }

    fun mark(text: String) {
        label.text = text
    }

    fun reset() {
        label.text = previous
    }

    private var previous: String? = null
}

open class IForm(title: String = "", val snap: Settings? = null) : JFrame(title) {
    val actions = HashMap<String, Action>()

    var toolBar: JToolBar? = null

    var statusBar: IStatusBar? = null

    var statusText: String get() = statusBar?.text ?: ""
        set(value) {
            statusBar?.text = value
        }
}
