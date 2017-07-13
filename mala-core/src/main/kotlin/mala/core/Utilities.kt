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

import jclp.setting.Settings
import jclp.value.Value
import jclp.value.Values
import kotlin.reflect.KProperty

infix fun String?.or(lazyString: () -> String): String = if (isNullOrEmpty()) lazyString() else this!!

fun Settings.mapped(default: Int, key: String = "") = SettingsMapping(Int::class.java, key, Values.wrap(default))

fun Settings.mapped(default: String, key: String = "") = SettingsMapping(String::class.java, key, Values.wrap(default))

fun Settings.mapped(default: Boolean, key: String = "") = SettingsMapping(Boolean::class.java, key, Values.wrap(default))

inline fun <reified T> Settings.mapped(default: Value<T>, key: String = "") = SettingsMapping(T::class.java, key, default)

class SettingsMapping<T>(val type: Class<T>, val key: String = "", val default: Value<T>) {
    operator fun setValue(settings: Settings, property: KProperty<*>, value: T) {
        settings.set(key or { property.name }, value, type)
    }

    operator fun getValue(settings: Settings, property: KProperty<*>): T {
        return settings.get(key or { property.name }, type) ?: default.get()
    }
}


