package mala.ixin

import jclp.util.Translator
import mala.core.App
import mala.core.ResourceManager
import mala.core.or
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.KeyStroke

@Suppress("unchecked_cast")
operator fun <T : Any> Action.get(name: String): T? = getValue(name) as? T

operator fun <T : Any> Action.set(name: String, value: T?) = putValue(name, value)

var Action.isSelected: Boolean get() = getValue(Action.SELECTED_KEY) == true
    set(value) = putValue(Action.SELECTED_KEY, value)

interface ActionInspector {
    fun inspect(action: Action)
}

abstract class IAction(id: String, translator: Translator, resources: ResourceManager) : AbstractAction() {
    companion object {
        const val SCOPE_KEY = "IxInScopeKey"
        const val SELECTED_ICON_KEY = "IxInSelectedIconKey"

        const val ICON_SUFFIX = ".icon"
        const val SCOPE_SUFFIX = ".scope"
        const val TOAST_SUFFIX = ".toast"
        const val DETAILS_SUFFIX = ".details"
        const val SHORTCUT_SUFFIX = ".shortcut"

        const val SHOWY_ICON_SUFFIX = "-showy"
        const val SELECTED_ICON_SUFFIX = "-selected"

        const val ICON_BASE = "gfx/actions"
        const val ICON_FORMAT = ".png"
    }

    init {
        putValue(Action.ACTION_COMMAND_KEY, id)
        var text = translator.optTr(id, "")
        if (text.isNotEmpty()) {
            val results = IxIn.getMnemonic(text)
            putValue(Action.NAME, results.name)
            if (results.isEnable) {
                putValue(Action.MNEMONIC_KEY, results.mnemonic)
                putValue(Action.DISPLAYED_MNEMONIC_INDEX_KEY, results.index)
            }
        }

        text = translator.optTr(id + ICON_SUFFIX, null) or { "${IxIn.iconSet}/$ICON_BASE/$id$ICON_FORMAT" }
        putValue(Action.SMALL_ICON, resources.iconFor(text))
        putValue(Action.LARGE_ICON_KEY, resources.iconFor(text + SHOWY_ICON_SUFFIX))
        putValue(SELECTED_ICON_KEY, resources.iconFor(text + SELECTED_ICON_SUFFIX))

        text = translator.optTr(id + SHORTCUT_SUFFIX, "")
        if (text.isNotEmpty()) {
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(text))
        }

        text = translator.optTr(id + TOAST_SUFFIX, "")
        if (text.isNotEmpty()) {
            putValue(Action.SHORT_DESCRIPTION, text)
        }

        text = translator.optTr(id + DETAILS_SUFFIX, "")
        if (text.isNotEmpty()) {
            putValue(Action.LONG_DESCRIPTION, text)
        }

        text = translator.optTr(id + SCOPE_SUFFIX, "")
        if (text.isNotEmpty()) {
            putValue(SCOPE_KEY, text)
        }
    }

    constructor(id: String) : this(id, App, App.resourceManager)

    override fun toString(): String {
        return keys.map {
            "$it='${getValue(it.toString())}'"
        }.joinToString(", ", "${javaClass.simpleName}@${hashCode()}{", "}")
    }
}

class SilentAction(id: String, translator: Translator, resources: ResourceManager) : IAction(id, translator, resources) {
    override fun actionPerformed(e: ActionEvent?) {
    }
}

class DispatcherAction(id: String,
                       translator: Translator,
                       resources: ResourceManager,
                       val listener: CommandListener) : IAction(id, translator, resources) {

    constructor(id: String, listener: CommandListener) : this(id, App, App.resourceManager, listener)

    override fun actionPerformed(e: ActionEvent) {
        listener.performed(e.actionCommand)
    }
}
