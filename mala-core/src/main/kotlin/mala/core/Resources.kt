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
import jclp.io.IOUtils.CLASS_PATH_PREFIX
import jclp.util.Linguist
import java.net.URL
import java.util.*

class ResourceManager(base: String, private val loader: ClassLoader? = null) {
    private val home: String

    init {
        var path: String = base
        if (path.startsWith("$CLASS_PATH_PREFIX/")) {
            path = "$CLASS_PATH_PREFIX${path.substring(2)}"
        }
        if (path != CLASS_PATH_PREFIX && !path.endsWith("/")) {
            path += "/"
        }
        home = path
    }

    fun resourceFor(name: String): URL? {
        return IOUtils.resourceFor(home + name.trimStart('/'))
    }

    fun linguistFor(name: String, locale: Locale? = null): Linguist {
        return Linguist((if (home.startsWith(CLASS_PATH_PREFIX)) home.substring(1) else home) + name, locale, loader)
    }

    override fun toString() = "ResourceManager(home='$home')"
}
