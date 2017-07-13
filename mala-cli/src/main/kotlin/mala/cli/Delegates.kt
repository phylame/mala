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

import mala.core.App
import mala.core.AppDelegate
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Option.Builder
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import java.util.LinkedList

abstract class CLIDelegate(val parser: CommandLineParser) : AppDelegate {
    val options = Options()

    var inputs = emptyArray<String>()
        private set

    val context = HashMap<String, Any>()

    private val commands = LinkedList<Command>()

    private val actions = HashMap<String, Action>()

    protected var defaultCommand: ((CLIDelegate) -> Int)? = null

    fun addAction(option: Option, action: Action) {
        options.addOption(option)
        actions[option.opt] = action
    }

    fun addAction(option: Option, action: (CLIDelegate) -> Int) {
        addAction(option, object : Command {
            override fun execute(delegate: CLIDelegate): Int = action(delegate)
        })
    }

    fun Builder.action(action: Action) {
        addAction(build(), action)
    }

    fun Builder.action(action: (CLIDelegate) -> Int) {
        addAction(build(), action)
    }

    protected open fun onOptionError(e: ParseException) {
        App.die("bad option", e)
    }

    protected open fun onOptionParsed(): Boolean = true

    override final fun run() {
        parseOptions()
        App.exit(if (onOptionParsed()) executeCommands() else -1)
    }

    private fun parseOptions() {
        try {
            val cmd = parser.parse(options, App.arguments)
            cmd.options.map {
                actions[it.opt]
            }.forEach {
                when (it) {
                    is Command -> {
                        commands += it
                    }
                    is Initializer -> {
                        it.perform(context, cmd)
                    }
                }
            }
            inputs = cmd.args
        } catch (e: ParseException) {
            onOptionError(e)
        }
    }

    private fun executeCommands(): Int {
        var code = 0
        if (commands.isNotEmpty()) {
            for (command in commands) {
                code = minOf(code, command.execute(this))
            }
        } else {
            code = defaultCommand?.invoke(this) ?: 0
        }
        return code
    }
}
