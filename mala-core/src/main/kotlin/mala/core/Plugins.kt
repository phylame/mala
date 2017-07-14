/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

package mala.core

import mala.core.App.error
import mala.core.App.optTr
import java.io.InputStream
import java.util.*

interface Plugin {
    val meta: Map<String, Any>

    fun init() {}

    fun destroy() {}
}

class PluginManager(private val path: String) {
    companion object {
        private const val COMMENT_LABEL = "#"
    }

    var isEnable: Boolean = true

    var loader: ClassLoader? = null

    var filter: ((Plugin) -> Boolean)? = null

    var blacklist: Collection<String> = emptySet()

    private val plugins = LinkedList<Plugin>()

    fun init() {
        if (!isEnable) {
            return
        }
        loadPlugins()
        plugins.forEach(Plugin::init)
    }

    fun <T : Plugin> with(type: Class<T>, action: (T) -> Unit) {
        if (!isEnable) {
            return
        }
        plugins.filter(type::isInstance)
                .map(type::cast)
                .forEach(action)
    }

    fun destroy() {
        if (!isEnable) {
            return
        }
        plugins.forEach(Plugin::destroy)
        plugins.clear()
    }

    private fun loadPlugins() {
        val loader = if (loader != null) loader!! else javaClass.classLoader
        for (url in loader.getResources(path)) {
            url.openStream().use {
                parseRegistry(it, loader)
            }
        }
    }

    private fun parseRegistry(input: InputStream, loader: ClassLoader) {
        input.bufferedReader().useLines {
            it.map(String::trim)
                    .filter { it.isNotEmpty() && !it.startsWith(COMMENT_LABEL) && it !in blacklist }
                    .forEach {
                        loadPlugin(it, loader)
                    }
        }
    }

    private fun loadPlugin(path: String, loader: ClassLoader) {
        try {
            val clazz = loader.loadClass(path)
            if (!Plugin::class.java.isAssignableFrom(clazz)) {
                error(optTr("mala.err.badPlugin", "plugin must be sub-class of ''{0}'': {1}", Plugin::class.java.name, path))
                return
            }
            val plugin = clazz.newInstance() as Plugin
            if (filter?.invoke(plugin) ?: true) {
                plugins += plugin
            }
        } catch (e: ReflectiveOperationException) {
            error(optTr("mala.err.loadPlugin", "cannot load plugin: {0}", path), e)
        }
    }
}

fun Plugin.tr(key: String): String = App.tr(key)

fun Plugin.tr(key: String, vararg args: Array<Any>): String = App.tr(key, *args)
