/*
 * Copyright 2015-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of IxIn.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mala.ixin

import jclp.io.IOUtils
import jclp.log.Log
import jclp.util.Translator
import mala.core.App
import mala.core.ResourceManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.awt.Component
import java.io.InputStream
import java.util.*
import javax.swing.*

enum class Style {
    PLAIN, RADIO, CHECK, TOGGLE;
}

open class Item(val id: String,
                val isEnable: Boolean = true,
                val isSelected: Boolean = false,
                val style: Style = Style.PLAIN) {
    init {
        require(id.isNotEmpty()) { "id of action cannot be empty" }
    }

    override fun hashCode() = id.hashCode()

    override fun equals(other: Any?) = other != null && other is Item && other.id == id

    override fun toString() = "${javaClass.simpleName}(id='$id', isEnable=$isEnable, isSelected=$isSelected, style=$style)"
}

fun Item.toAction(handler: CommandHandler,
                  translator: Translator = App,
                  resources: ResourceManager = App.resourceManager): Action {
    require(this !== Separator) { "Separator cannot be created as action" }
    require(this !is Group) { "Group cannot be created as action" }
    val action = CommandAction(id, handler, translator, resources)
    action.isSelected = isSelected
    action.isEnabled = isEnable
    return action
}

fun Action.toButton(style: Style = Style.PLAIN, inspector: ComponentInspector? = null): AbstractButton {
    fallbackName()
    val button: AbstractButton = when (style) {
        Style.PLAIN -> JButton(this)
        Style.CHECK -> JCheckBox(this)
        Style.RADIO -> JRadioButton(this)
        Style.TOGGLE -> JToggleButton(this)
    }
    button.update(this)
    if (inspector != null) {
        button.toolTipText = null
        button.inspect(inspector, this)
    }
    return button
}

fun Action.toImageButton(style: Style = Style.PLAIN, inspector: ComponentInspector? = null): AbstractButton {
    val button = toButton(style, inspector)
    button.isContentAreaFilled = false
    button.isRolloverEnabled = false
    button.isBorderPainted = false
    button.hideActionText = true
    return button
}

fun Action.toMenuItem(style: Style = Style.PLAIN, inspector: ComponentInspector? = null): JMenuItem {
    fallbackName()
    val item = when (style) {
        Style.PLAIN -> JMenuItem(this)
        Style.CHECK -> JCheckBoxMenuItem(this)
        Style.RADIO -> JRadioButtonMenuItem(this)
        Style.TOGGLE -> throw IllegalArgumentException("Style of toggle is not supported for menu item")
    }
    item.update(this)
    if (inspector != null) {
        item.toolTipText = null
        item.inspect(inspector, this)
    }
    return item
}

fun ActionMap.get(id: String,
                  handler: CommandHandler,
                  translator: Translator = App,
                  resources: ResourceManager = App.resourceManager): Action {
    return get(Item(id), handler, translator, resources)
}

fun ActionMap.get(item: Item,
                  handler: CommandHandler,
                  translator: Translator = App,
                  resources: ResourceManager = App.resourceManager): Action {
    var action = this[item.id]
    if (action == null) {
        action = item.toAction(handler, translator, resources)
        put(item.id, action)
    }
    return action
}

fun ActionMap.updateKeys(keymap: Map<String, KeyStroke?>, resetOthers: Boolean = false) {
    if (resetOthers) {
        for (key in this.keys()) {
            get(key)?.putValue(Action.ACCELERATOR_KEY, null)
        }
    }
    for ((id, key) in keymap.entries) {
        val action = get(id)
        if (action != null) {
            action.putValue(Action.ACCELERATOR_KEY, key)
            var tip: String? = action[Action.SHORT_DESCRIPTION]
            if (tip != null && tip.isNotEmpty()) {
                if (key != null) {
                    tip += " (${key.name})"
                }
//                action[Action.SHORT_DESCRIPTION] = tip
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun ResourceManager.keymapFor(name: String): Map<String, KeyStroke> {
    val values = propertiesFor(name) ?: return emptyMap()
    val iterator = values.entries.iterator()
    while (iterator.hasNext()) {
        val entry = iterator.next()
        val stroke = KeyStroke.getKeyStroke(entry.value as String)
        if (stroke == null) {
            App.error("invalid key stroke: '${entry.value}'")
            iterator.remove()
        } else {
            entry.setValue(stroke)
        }
    }
    return values as Map<String, KeyStroke>
}

object Separator : Item("__SEPARATOR__")

open class Group(id: String, val items: Array<Item>) : Item(id)

fun Group.toMenu(translator: Translator = App, resources: ResourceManager = App.resourceManager): JMenu {
    val menu = JMenu(SilentAction(id, translator, resources).fallbackName())
    menu.toolTipText = null
    return menu
}

interface Designer {
    val menubar: Array<Group>?

    val toolbar: Array<Item>?
}

class BadDesignerException(message: String) : Exception(message)

open class JSONDesigner(input: InputStream) : Designer {
    constructor(path: String) : this(IOUtils.openResource(path) ?: throw BadDesignerException("Not such resource: $path"))

    private val menuGroups = LinkedList<Group>()

    private val toolItems = LinkedList<Item>()

    init {
        val json = JSONObject(JSONTokener(input))
        try {
            for (item in json.getJSONArray(MENUBAR_KEY)) {
                if (item !is JSONObject) {
                    throw BadDesignerException("Require 'object' in $MENUBAR_KEY")
                }
                val id = item.getString("id")
                val items = LinkedList<Item>()
                val array = item.optJSONArray("items")
                if (array != null) {
                    parseItems(array, items)
                }
                menuGroups += Group(id, items.toTypedArray())
            }
        } catch (e: JSONException) {
            Log.d(TAG, "bad designer file", e)
        }
        try {
            parseItems(json.getJSONArray(TOOLBAR_KEY), toolItems)
        } catch (e: JSONException) {
            Log.d(TAG, "bad designer file", e)
        }
    }

    override val menubar: Array<Group> = menuGroups.toTypedArray()

    override val toolbar: Array<Item> = toolItems.toTypedArray()

    companion object {
        private const val TAG = "JSONDesigner"
        const val TOOLBAR_KEY = "toolbar"
        const val MENUBAR_KEY = "menubar"

        fun parseItems(array: JSONArray, items: MutableCollection<Item>) {
            for (item in array) {
                when (item) {
                    is String -> items += Item(item)
                    is JSONObject -> {
                        val id = item.getString("id")
                        val subarray = item.optJSONArray("items")
                        if (subarray != null) {
                            val subitems = LinkedList<Item>()
                            parseItems(subarray, subitems)
                            items += Group(id, subitems.toTypedArray())
                        } else {
                            items += Item(id,
                                    item.optBoolean("enable", true),
                                    item.optBoolean("selected", false),
                                    Style.valueOf(item.optString("style", Style.PLAIN.name).toUpperCase()))
                        }
                    }
                    JSONObject.NULL -> items += Separator
                    else -> throw BadDesignerException("Unexpected style of item: ${item.javaClass}")
                }
            }
        }
    }
}

fun ResourceManager.designerFor(name: String): Designer? {
    return JSONDesigner(resourceFor(if (name.endsWith(".json")) name else name + ".json")?.openStream() ?: return null)
}

fun <T : JMenu> T.attach(items: Array<*>,
                         actions: ActionMap,
                         handler: CommandHandler,
                         translator: Translator = App,
                         resources: ResourceManager = App.resourceManager,
                         inspector: ComponentInspector? = null): T {
    popupMenu.attach(items, actions, handler, translator, resources, inspector)
    return this
}

fun <T : JPopupMenu> T.attach(items: Array<*>,
                              actions: ActionMap,
                              handler: CommandHandler,
                              translator: Translator = App,
                              resources: ResourceManager = App.resourceManager,
                              inspector: ComponentInspector? = null): T {
    var group: ButtonGroup? = null
    for (item in items) {
        val comp: Component = when (item) {
            is Component -> item
            null, Separator -> JPopupMenu.Separator()
            is String -> actions.get(item, handler, translator, resources).toMenuItem(Style.PLAIN, inspector)
            is Group -> item.toMenu(translator, resources).attach(item.items, actions, handler, translator, resources, inspector)
            is Item -> {
                val result = actions.get(item, handler, translator, resources).toMenuItem(item.style, inspector)
                if (item.style == Style.RADIO) {
                    if (group == null) {
                        group = ButtonGroup()
                    }
                    group.add(result)
                } else if (group != null) {
                    group = null
                }
                result
            }
            else -> throw IllegalArgumentException("Illegal item")
        }
        add(comp)
    }
    return this
}

fun <T : JToolBar> T.attach(items: Array<*>,
                            actions: ActionMap,
                            handler: CommandHandler,
                            translator: Translator = App,
                            resources: ResourceManager = App.resourceManager,
                            inspector: ComponentInspector? = null): T {
    var group: ButtonGroup? = null
    for (item in items) {
        when (item) {
            is Component -> add(item)
            null, Separator -> addSeparator()
            is Item -> {
                val button = actions.get(item.id, handler, translator, resources).toButton(item.style, inspector)
                attach(button)
                if (item.style == Style.RADIO) {
                    if (group == null) {
                        group = ButtonGroup()
                    }
                    group.add(button)
                } else if (group != null) {
                    group = null
                }
            }
            is String -> attach(actions.get(item, handler, translator, resources).toButton(Style.PLAIN, inspector))
        }
    }
    return this
}
