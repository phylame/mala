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

package mala.cli

import jclp.text.Converters
import mala.core.App
import org.apache.commons.cli.CommandLine

typealias Validator<T> = (T) -> Boolean
typealias AppContext = MutableMap<String, Any>

interface Action

interface Command : Action {
    fun execute(delegate: CLIDelegate): Int
}

interface Initializer : Action {
    fun perform(context: AppContext, cmd: CommandLine)
}

interface ValueFetcher<T : Any> : Initializer {
    val name: String

    fun parse(str: String): T

    val validator: Validator<T>?

    override fun perform(context: AppContext, cmd: CommandLine) {
        val value = parse(cmd.getOptionValue(name))
        if (validator?.invoke(value) ?: true) {
            context[name] = value
        }
    }
}

class RawFetcher(override val name: String,
                 override val validator: ((String) -> Boolean)? = null) : ValueFetcher<String> {
    override fun parse(str: String) = str
}

class TypedFetcher<T : Any>(val type: Class<T>,
                            override val name: String,
                            override val validator: ((T) -> Boolean)? = null) : ValueFetcher<T> {
    override fun parse(str: String): T {
        try {
            return Converters.parse(str, type) ?: App.die("cannot convert $str to $type")
        } catch (e: RuntimeException) {
            App.die("cannot convert input '$str' to '$type'", e)
        }
    }
}

inline fun <reified T : Any> fetcherFor(name: String) = TypedFetcher(T::class.java, name)

abstract class SingleFetcher : Initializer {
    private var isPerformed = false

    protected abstract fun init(context: AppContext, cmd: CommandLine)

    override final fun perform(context: AppContext, cmd: CommandLine) {
        if (!isPerformed) {
            init(context, cmd)
            isPerformed = true
        }
    }
}

open class ValuesFetcher(val option: String) : SingleFetcher() {
    override fun init(context: AppContext, cmd: CommandLine) {
        context[option] = cmd.getOptionValues(option).toList()
    }
}

class PropertiesFetcher(val option: String) : SingleFetcher() {
    override fun init(context: AppContext, cmd: CommandLine) {
        context[option] = cmd.getOptionProperties(option)
    }
}

class ValueSwitcher(val name: String) : Initializer {
    override fun perform(context: AppContext, cmd: CommandLine) {
        context[name] = true
    }
}
