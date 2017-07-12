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
import mala.core.AppVerbose
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option

object CLI : CLIDelegate(DefaultParser()) {
    override val name = "testApp"

    override val version = "1.0"

    override fun onStart() {
        App.verbose = AppVerbose.TRACE
        addAction(Option.builder("o")
                .hasArg()
                .desc("specified the output path")
                .build(), RawFetcher("o"))
        addAction(Option.builder("f")
                .hasArg()
                .desc("specified the input format")
                .build(), RawFetcher("f"))
        addAction(Option.builder("t")
                .hasArg()
                .desc("specified the output format")
                .build(), RawFetcher("t"))
        addAction(Option.builder("T")
                .hasArg()
                .desc("specified the number of threads")
                .build(), fetcherFor<Int>("T"))
        addAction(Option.builder("a")
                .numberOfArgs(2)
                .argName("xxxx")
                .build(), PropertiesFetcher("a"))
        addAction(Option.builder("x").build(), ValueSwitcher("x"))
        addAction(Option.builder("c")
                .desc("convert book")
                .build()) {
            println(context)
            0
        }
    }
}

fun main(args: Array<String>) {
    App.run(CLI, args)
}
