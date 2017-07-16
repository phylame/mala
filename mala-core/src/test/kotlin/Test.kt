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

import jclp.setting.PropertiesSettings
import jclp.util.Linguist
import mala.core.*

object MySettings : PropertiesSettings(App.pathOf("app").reader(), null) {
    var name by map("N/A")
    var age by map(-1)
    var sex by map(true, "sex_1")
}

object MyDelegate : AppDelegate {

    override val name: String = "testApp"

    override val version: String = "1.0"

    val rm = ResourceManager("!")

    override fun onStart() {
        App.verbose = AppVerbose.TRACE
        App.setTranslator(Linguist("x"))
        println(MySettings.age)

        MySettings.age = 3456
        println(MySettings.sex)
        MySettings.sex = false
        println("a"*0)
    }

    override fun run() {
        App.exit()
    }
}

fun main(args: Array<String>) {
    App.run(MyDelegate, args)
}
