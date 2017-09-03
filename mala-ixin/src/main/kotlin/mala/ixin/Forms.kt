package mala.ixin

import jclp.setting.Settings
import jclp.util.Translator
import mala.core.App
import mala.core.ResourceManager
import org.jdesktop.swingx.JXFrame
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

open class IForm(title: String = "", val snap: Settings? = null) : JXFrame(title), ComponentInspector {
    val actions = ActionMap()

    var statusBar: IStatusBar? = null

    val topMenus = HashMap<String, JMenu>()

    var statusText: String
        get() = statusBar?.text ?: ""
        set(value) {
            statusBar?.text = value
        }

    protected fun init(designer: Designer) {
        init(designer, IxIn.delegate, App, App.resourceManager)
    }

    protected fun init(designer: Designer, handler: CommandHandler, translator: Translator, resources: ResourceManager) {
        iconImage = App.resourceManager.imageFor("icon")
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                IxIn.delegate.performed("exit")
            }
        })
        if (designer.menubar?.isNotEmpty() == true) {
            createMenuBar(designer.menubar!!, handler, translator, resources)
        }
        if (designer.toolbar?.isNotEmpty() == true) {
            createToolBar(designer.toolbar!!, handler, translator, resources)
        }
        createStatusBar()
    }

    override fun dispose() {
        saveState()
        super.dispose()
    }

    open protected fun restoreState() {
    }

    open protected fun saveState() {
    }

    open protected fun createMenuBar(menus: Array<Group>, handler: CommandHandler, translator: Translator, resources: ResourceManager) {
        val menuBar = JMenuBar()
        for (group in menus) {
            val menu = group.toMenu(translator, resources)
            menu.attach(group.items, actions, handler, translator, resources, this)
            topMenus[group.id] = menu
            menuBar.add(menu)
        }
        if (menuBar.menuCount > 0) {
            jMenuBar = menuBar
        }
    }

    open protected fun createToolBar(items: Array<*>, handler: CommandHandler, translator: Translator, resources: ResourceManager) {
        toolBar = JToolBar(title)
        contentPane.add(toolBar, BorderLayout.PAGE_START)
        toolBar?.isRollover = true
        toolBar?.attach(items, actions, handler, translator, resources, this)
        val popupMenu = JPopupMenu()
        val menuItem = Item("lockToolbar", style = Style.CHECK)
                .toAction(handler, translator, resources)
                .toMenuItem(Style.CHECK, this)
        popupMenu.add(menuItem)
        toolBar?.componentPopupMenu = popupMenu
    }

    open protected fun createStatusBar() {
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

fun IForm.getOrPut(id: String): Action = actions.get(id, IxIn.delegate, App, App.resourceManager)

operator fun IForm.get(id: String): Action? = actions[id]

operator fun IForm.set(id: String, enable: Boolean) {
    setEnable(id, enable)
}

fun IForm.isEnable(id: String) = actions[id]?.isEnabled == true

fun IForm.setEnable(id: String, enable: Boolean) {
    actions[id]?.isEnabled = enable
}

class IStatusBar : JPanel(BorderLayout()) {
    val label: JLabel = JLabel()

    var text: String
        get() = label.text
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
