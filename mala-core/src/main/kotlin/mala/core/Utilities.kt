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

import jclp.io.IOUtils
import jclp.setting.PropertiesSettings
import jclp.setting.Settings
import jclp.util.StringUtils
import jclp.value.Value
import jclp.value.Values
import java.io.File
import kotlin.reflect.KProperty

fun String.titled(): String = StringUtils.titled(this)

operator fun String.times(n: Int): String {
    val sb = StringBuilder(length * n)
    for (i in 1..n) {
        sb.append(this)
    }
    return sb.toString()
}

infix fun String?.or(lazyString: () -> String): String = if (isNullOrEmpty()) lazyString() else this!!

operator fun StringBuilder.plusAssign(str: String) {
    append(str)
}

operator fun StringBuilder.plusAssign(obj: Any) {
    append(obj)
}

fun <E : Iterable<E>> E.walk(action: E.(Int, Int) -> Unit) {
    walkInternal(0, 0, action)
}

private fun <E : Iterable<E>> E.walkInternal(level: Int, index: Int, action: E.(Int, Int) -> Unit) {
    action(level, index)
    val l = level + 1
    for ((i, e) in this.withIndex()) {
        e.walkInternal(l, i, action)
    }
}

fun map(default: Int, key: String = "") = SettingsMapping(Int::class.java, key, Values.wrap(default))

fun map(default: String, key: String = "") = SettingsMapping(String::class.java, key, Values.wrap(default))

fun map(default: Boolean, key: String = "") = SettingsMapping(Boolean::class.java, key, Values.wrap(default))

inline fun <reified T> map(default: Value<T>, key: String = "") = SettingsMapping(T::class.java, key, default)

class SettingsMapping<T>(val type: Class<T>, val key: String = "", val default: Value<T>) {
    operator fun setValue(settings: Settings, property: KProperty<*>, value: T) {
        settings.set(key or { property.name }, value)
    }

    operator fun getValue(settings: Settings, property: KProperty<*>): T {
        return settings.get(key or { property.name }, type) ?: default.get()
    }
}

open class AppSettings(name: String = "", loading: Boolean = true, autosync: Boolean = true) : PropertiesSettings() {
    val file = File(App.pathOf(name))

    init {
        if (loading && file.exists()) {
            IOUtils.readerFor(file).use {
                load(it)
            }
        }
        if (autosync) {
            App.registerCleanup {
                if (App.initAppHome()) {
                    IOUtils.writerFor(file).use {
                        sync(it)
                    }
                } else {
                    App.error("cannot create app home")
                }
            }
        }
    }
}
