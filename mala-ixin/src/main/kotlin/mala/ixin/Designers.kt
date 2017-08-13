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

fun Item.toAction(listener: CommandListener,
                  translator: Translator = App,
                  resources: ResourceManager = App.resourceManager): Action {
    require(this !== Separator) { "Separator cannot be created as action" }
    require(this !is Group) { "Group cannot be created as action" }
    val action = CommandAction(id, listener, translator, resources)
    action.isSelected = isSelected
    action.isEnabled = isEnable
    return action
}

fun Action.toButton(style: Style, inspector: ComponentInspector? = null): AbstractButton {
    fallbackName()
    val button: AbstractButton = when (style) {
        Style.PLAIN -> JButton(this)
        Style.CHECK -> JCheckBox(this)
        Style.RADIO -> JRadioButton(this)
        Style.TOGGLE -> JToggleButton(this)
    }
    button.update(this)
    button.toolTipText = null
    if (inspector != null) {
        button.inspect(inspector, this)
    }
    return button
}

fun Action.toMenuItem(style: Style, inspector: ComponentInspector? = null): JMenuItem {
    fallbackName()
    val item = when (style) {
        Style.PLAIN -> JMenuItem(this)
        Style.CHECK -> JCheckBoxMenuItem(this)
        Style.RADIO -> JRadioButtonMenuItem(this)
        Style.TOGGLE -> throw IllegalArgumentException("Style of toggle is not supported for menu item")
    }
    item.update(this)
    item.toolTipText = null
    if (inspector != null) {
        item.inspect(inspector, this)
    }
    return item
}

fun ActionMap.get(id: String,
                  listener: CommandListener,
                  translator: Translator = App,
                  resources: ResourceManager = App.resourceManager): Action {
    return get(Item(id), listener, translator, resources)
}

fun ActionMap.get(item: Item,
                  listener: CommandListener,
                  translator: Translator = App,
                  resources: ResourceManager = App.resourceManager): Action {
    var action = this[item.id]
    if (action == null) {
        action = item.toAction(listener, translator, resources)
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
        get(id)?.putValue(Action.ACCELERATOR_KEY, key)
    }
}

@Suppress("UNCHECKED_CAST")
fun ResourceManager.keymapFor(name: String): Map<String, KeyStroke> {
    val values = propertiesFor(name) ?: return emptyMap()
    val iterator = values.entries.iterator()
    while (iterator.hasNext()) {
        val entry = iterator.next()
        val stroke = IxIn.getKeyStroke(entry.value as String)
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

fun <T : JMenu> T.attach(items: Array<Item>,
                         actions: ActionMap,
                         listener: CommandListener,
                         translator: Translator = App,
                         resources: ResourceManager = App.resourceManager,
                         inspector: ComponentInspector? = null): T {
    popupMenu.attach(items, actions, listener, translator, resources, inspector)
    return this
}

fun <T : JPopupMenu> T.attach(items: Array<out Item>,
                              actions: ActionMap,
                              listener: CommandListener,
                              translator: Translator = App,
                              resources: ResourceManager = App.resourceManager,
                              inspector: ComponentInspector? = null): T {
    var group: ButtonGroup? = null
    for (item in items) {
        val comp: JComponent = when (item) {
            Separator -> JPopupMenu.Separator()
            is Group -> item.toMenu(translator, resources).attach(item.items, actions, listener, translator, resources, inspector)
            else -> {
                val result = actions.get(item, listener, translator, resources).toMenuItem(item.style, inspector)
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
        }
        add(comp)
    }
    return this
}

fun <T : JToolBar> T.attach(items: Array<*>,
                            actions: ActionMap,
                            listener: CommandListener,
                            translator: Translator = App,
                            resources: ResourceManager = App.resourceManager,
                            inspector: ComponentInspector? = null): T {
    var group: ButtonGroup? = null
    for (item in items) {
        when (item) {
            Separator -> addSeparator()
            is Item -> {
                val button = actions.get(item.id, listener, translator, resources).toButton(item.style, inspector)
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
            is Component -> add(item)
        }
    }
    return this
}
