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

private typealias Validator<T> = (T) -> Boolean
private typealias AppContext = MutableMap<String, Any>

interface Action

interface Command : Action {
    fun execute(delegate: CLIDelegate): Int
}

interface Initializer : Action {
    fun perform(context: AppContext, cmd: CommandLine)
}

class Switcher(val name: String) : Initializer {
    override fun perform(context: AppContext, cmd: CommandLine) {
        context[name] = true
    }
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

class StringFetcher(override val name: String,
                    override val validator: ((String) -> Boolean)? = null) : ValueFetcher<String> {
    override fun parse(str: String) = str
}

class ConverterFetcher<T : Any>(val type: Class<T>,
                                override val name: String,
                                override val validator: ((T) -> Boolean)? = null) : ValueFetcher<T> {
    override fun parse(str: String): T {
        try {
            return Converters.parse(str, type, true) ?: App.die("cannot convert $str to $type")
        } catch (e: RuntimeException) {
            App.die("cannot convert input '$str' to '$type'", e)
        }
    }
}

inline fun <reified T : Any> fetcherFor(name: String) = ConverterFetcher(T::class.java, name)

abstract class SingleInitializer : Initializer {
    private var performed = false

    protected abstract fun init(context: AppContext, cmd: CommandLine)

    override fun perform(context: AppContext, cmd: CommandLine) {
        if (!performed) {
            init(context, cmd)
            performed = true
        }
    }
}

open class ListFetcher(val option: String) : SingleInitializer() {
    override fun init(context: AppContext, cmd: CommandLine) {
        context[option] = cmd.getOptionValues(option)
    }
}

open class PropertiesFetcher(val option: String) : SingleInitializer() {
    override fun init(context: AppContext, cmd: CommandLine) {
        context[option] = cmd.getOptionProperties(option)
    }
}
