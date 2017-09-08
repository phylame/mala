package mala.core

import pw.phylame.commons.i18n.Linguist
import pw.phylame.commons.io.ResourceUtils
import java.util.*
import pw.phylame.commons.io.ResourceUtils.CLASSPATH_PREFIX as PREFIX

class AssetManager(base: String, private val loader: ClassLoader? = null) {
    private val home: String

    init {
        require(base.isNotEmpty()) { "base path cannot be empty" }
        val path = if (base.startsWith("$PREFIX/")) PREFIX + base.substring(1 + PREFIX.length) else base
        home = if (path != PREFIX && !path.endsWith("/")) path + '/' else path
    }

    fun pathOf(name: String) = home + name.trimStart('/')

    fun resourceFor(name: String) = ResourceUtils.getResource(pathOf(name), loader)

    fun propertiesFor(name: String, reload: Boolean = false) = ResourceUtils.getProperties(pathOf(name), loader, reload)

    fun translatorFor(name: String, locale: Locale? = null, dummy: Boolean = true) = Linguist(pathOf(name), locale, loader, dummy)

    override fun toString() = "AssetManager(home='$home')"
}