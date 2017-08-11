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

fun <T : Action> T.fallbackName(): T {
    val name = getValue(Action.NAME) as? String
    if (name.isNullOrEmpty()) {
        putValue(Action.NAME, getValue(Action.ACTION_COMMAND_KEY))
    }
    return this
}

abstract class IAction(id: String,
                       translator: Translator = App,
                       resources: ResourceManager = App.resourceManager) : AbstractAction() {
    companion object {
        const val SCOPE_KEY = "IxInScopeKey"
        const val PRESSED_ICON_KEY = "IxInPressedIconKey"
        const val SELECTED_ICON_KEY = "IxInSelectedIconKey"
        const val ROLLOVER_ICON_KEY = "IxInRolloverIconKey"
        const val ROLLOVER_SELECTED_ICON_KEY = "IxInRolloverSelectedIconKey"
        const val DISABLED_ICON_KEY = "IxInDisabledIconKey"
        const val DISABLED_SELECTED_ICON_KEY = "IxInDisabledSelectedIconKey"

        const val ICON_SUFFIX = ".icon"
        const val SCOPE_SUFFIX = ".scope"
        const val TOAST_SUFFIX = ".toast"
        const val DETAILS_SUFFIX = ".details"
        const val SHORTCUT_SUFFIX = ".shortcut"

        const val SHOWY_ICON = "-showy"
        const val PRESSED_ICON = "-pressed"
        const val SELECTED_ICON = "-selected"
        const val ROLLOVER_ICON = "-rollover"
        const val ROLLOVER_SELECTED_ICON = "-rollover-selected"
        const val DISABLED_ICON = "-disabled"
        const val DISABLED_SELECTED_ICON = "-disabled-selected"

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

        text = translator.optTr(id + ICON_SUFFIX, null) or { "actions/$id" }
        text = "gfx/${if (IxIn.iconSet.isEmpty()) "" else IxIn.iconSet + '/'}$text"
        putValue(Action.SMALL_ICON, resources.iconFor(text + ICON_FORMAT))
        putValue(Action.LARGE_ICON_KEY, resources.iconFor(text + SHOWY_ICON + ICON_FORMAT))
        putValue(PRESSED_ICON_KEY, resources.iconFor(text + PRESSED_ICON + ICON_FORMAT))
        putValue(SELECTED_ICON_KEY, resources.iconFor(text + SELECTED_ICON + ICON_FORMAT))
        putValue(ROLLOVER_ICON_KEY, resources.iconFor(text + ROLLOVER_ICON + ICON_FORMAT))
        putValue(ROLLOVER_SELECTED_ICON_KEY, resources.iconFor(text + ROLLOVER_SELECTED_ICON + ICON_FORMAT))
        putValue(DISABLED_ICON_KEY, resources.iconFor(text + DISABLED_ICON + ICON_FORMAT))
        putValue(DISABLED_SELECTED_ICON_KEY, resources.iconFor(text + DISABLED_SELECTED_ICON + ICON_FORMAT))

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

    override fun toString(): String {
        return keys.map { "$it='${getValue(it.toString())}'" }.joinToString(", ", "${javaClass.simpleName}@${hashCode()}{", "}")
    }
}

class SilentAction(id: String,
                   translator: Translator = App,
                   resources: ResourceManager = App.resourceManager) : IAction(id, translator, resources) {
    override fun actionPerformed(e: ActionEvent?) {
    }
}

class CommandAction(id: String,
                    val listener: CommandListener,
                    translator: Translator = App,
                    resources: ResourceManager = App.resourceManager) : IAction(id, translator, resources) {
    override fun actionPerformed(e: ActionEvent) {
        listener.performed(e.actionCommand)
    }
}
