package mala.ixin

import jclp.setting.Settings
import jclp.util.Translator
import mala.core.ResourceManager
import java.awt.BorderLayout
import javax.swing.*

open class IForm(title: String = "", val snap: Settings? = null) : JFrame(title), ComponentInspector {
    val actions = ActionMap()

    var toolBar: JToolBar? = null

    var statusBar: IStatusBar? = null

    val topMenus = HashMap<String, JMenu>()

    var statusText: String get() = statusBar?.text ?: ""
        set(value) {
            statusBar?.text = value
        }

    protected fun init(designer: Designer, listener: CommandListener, translator: Translator, resources: ResourceManager) {
        if (designer.menubar?.isNotEmpty() ?: false) {
            createMenuBar(designer.menubar!!, listener, translator, resources)
            if (designer.toolbar?.isNotEmpty() ?: false) {
                createToolBar(designer.toolbar!!, listener, translator, resources)
            }
        }
        createStatusBar()
        initActions()
    }

    override fun dispose() {
        saveState()
        super.dispose()
    }

    open protected fun initActions() {
    }

    open protected fun restoreState() {
    }

    open protected fun saveState() {
    }

    private fun createMenuBar(menus: Array<Group>, listener: CommandListener, translator: Translator, resources: ResourceManager) {
        val menuBar = JMenuBar()
        for (group in menus) {
            val menu = group.toMenu(translator, resources)
            menu.attach(group.items, actions, listener, translator, resources, this)
            topMenus[group.id] = menu
            menuBar.add(menu)
        }
        if (menuBar.menuCount > 0) {
            jMenuBar = menuBar
        }
    }

    private fun createToolBar(items: Array<*>, listener: CommandListener, translator: Translator, resources: ResourceManager) {
        toolBar = JToolBar(title)
        toolBar?.isRollover = true
        toolBar?.attach(items, actions, listener, translator, resources, this)
        contentPane.add(toolBar, BorderLayout.PAGE_START)
    }

    private fun createStatusBar() {
        statusBar = IStatusBar()
        contentPane.add(statusBar, BorderLayout.PAGE_END)
    }

    override fun inspect(text: String) {
        statusBar?.toast(text)
    }

    override fun reset() {
        statusBar?.reset()
    }
}

class IStatusBar : JPanel(BorderLayout()) {
    val label: JLabel = JLabel()

    var text: String get() = label.text
        set (value) {
            previous = value
            label.text = value
        }

    init {
        add(label, BorderLayout.LINE_START)
    }

    fun toast(text: String) {
        label.text = text
    }

    fun reset() {
        label.text = previous
    }

    private var previous: String? = null
}
