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
import org.apache.commons.cli.*
import org.apache.commons.cli.Option.Builder
import kotlin.collections.set

abstract class CDelegate(val parser: CommandLineParser) : AppDelegate {
    val options = Options()

    var inputs = emptyList<String>()
        private set

    val context = HashMap<String, Any>()

    private val commands = LinkedHashSet<Command>()

    private val actions = HashMap<String, Action>()

    protected var defaultCommand: Command? = null

    fun addAction(option: Option, action: Action) {
        options.addOption(option)
        actions[option.opt ?: option.longOpt] = action
    }

    fun addAction(option: Option, action: (CDelegate) -> Int) {
        addAction(option, object : Command {
            override fun execute(delegate: CDelegate): Int = action(delegate)
        })
    }

    protected open fun onOptionError(e: ParseException) {
        val msg = when (e) {
            is UnrecognizedOptionException -> App.tr("error.opt.unknown", e.option)
            is AlreadySelectedException -> App.tr("error.opt.selected", e.option.opt, e.optionGroup.selected)
            is MissingArgumentException -> App.tr("error.opt.noArg", e.option.opt)
            is MissingOptionException -> App.tr("error.opt.noOption", e.missingOptions.joinToString(", "))
            else -> return
        }
        App.die(msg)
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
                actions[it.opt ?: it.longOpt]
            }.forEach {
                if (it is Initializer) {
                    it.perform(context, cmd)
                }
                if (it is Command) {
                    commands += it
                }
            }
            inputs = cmd.args.asList()
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
            code = defaultCommand?.execute(this) ?: 0
        }
        return code
    }
}

fun Option.group(group: OptionGroup) {
    group.addOption(this)
}

fun Builder.action(action: Action): Option = build().apply {
    (App.delegate as CDelegate).addAction(this, action)
}

fun Builder.action(action: (CDelegate) -> Int): Option = build().apply {
    (App.delegate as CDelegate).addAction(this, action)
}

fun Builder.action(action: (AppContext, CommandLine) -> Unit): Option = build().apply {
    (App.delegate as CDelegate).addAction(this, object : Initializer {
        override fun perform(context: AppContext, cmd: CommandLine) {
            action(context, cmd)
        }
    })
}
