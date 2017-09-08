package mala.core

import mala.core.App.error
import mala.core.App.optTr
import java.io.InputStream
import java.util.*

interface Plugin {
    fun init() {}

    fun destroy() {}
}

class PluginManager(private val path: String, private val loader: ClassLoader? = null) : Iterable<Plugin> {
    companion object {
        private const val COMMENT_LABEL = "#"
    }

    var isEnable: Boolean = true

    var filter: ((Plugin) -> Boolean)? = null

    var blacklist: Collection<String> = emptySet()

    private val plugins = LinkedList<Plugin>()

    fun init() {
        if (isEnable) {
            parseRegistries()
            plugins.forEach(Plugin::init)
        }
    }

    fun <T : Plugin> with(type: Class<T>, action: T.() -> Unit) {
        if (isEnable) {
            plugins.filter(type::isInstance).map(type::cast).forEach(action)
        }
    }

    fun destroy() {
        if (isEnable) {
            plugins.forEach(Plugin::destroy)
            plugins.clear()
        }
    }

    override fun iterator(): Iterator<Plugin> = plugins.iterator()

    private fun parseRegistries() {
        val loader = loader ?: App.javaClass.classLoader
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
            val plugin: Plugin = try {
                clazz.getField("INSTANCE").get(null) as Plugin
            } catch (ignored: ReflectiveOperationException) {
                clazz.newInstance() as Plugin
            }
            if (filter?.invoke(plugin) != false) {
                plugins += plugin
            }
        } catch (e: ReflectiveOperationException) {
            error(optTr("mala.err.loadPlugin", "cannot load plugin: {0}", path), e)
        }
    }
}