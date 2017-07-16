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

import jclp.util.TranslatorWrapper
import java.io.File

typealias Cleanup = () -> Unit

enum class AppVerbose {
    NONE,
    ECHO,
    TRACE
}

enum class AppStatus {
    DEFAULT,
    STARTING,
    RUNNING,
    STOPPING
}

interface AppDelegate : Runnable {
    val name: String

    val version: String

    val resources: String get() = "!${javaClass.`package`?.name?.replace('.', '/').orEmpty()}/res"

    fun onStart() {}

    fun onStop() {}
}

object App : TranslatorWrapper() {
    private const val MALA_HOME_KEY = "mala.home"
    private const val PLUGIN_REGISTRY_PATH = "META-INF/mala/plugin.prop"

    var code: Int = 0
        private set

    var verbose = AppVerbose.ECHO

    lateinit var delegate: AppDelegate
        private set

    lateinit var arguments: Array<String>
        private set

    var status: AppStatus = AppStatus.DEFAULT
        private set

    private val cleanups = LinkedHashSet<Cleanup>()

    val pluginManager = PluginManager(PLUGIN_REGISTRY_PATH)

    lateinit var resourceManager: ResourceManager
        private set

    val home: String by lazy {
        System.getProperty(MALA_HOME_KEY) or { "${System.getProperty("user.home")}/.${delegate.name}" }
    }

    fun pathOf(name: String) = "$home/$name"

    fun run(delegate: AppDelegate, args: Array<String>) {
        this.arguments = args
        this.delegate = delegate
        resourceManager = ResourceManager(delegate.resources)
        status = AppStatus.STARTING
        delegate.onStart()
        pluginManager.init()
        status = AppStatus.RUNNING
        delegate.run()
    }

    fun exit(code: Int = 0): Nothing {
        this.code = code
        status = AppStatus.STOPPING
        delegate.onStop()
        pluginManager.destroy()
        cleanups.forEach(Cleanup::invoke)
        System.exit(this.code)
        throw InternalError()
    }

    fun echo(msg: Any) {
        System.out.println("${delegate.name}: $msg")
    }

    fun error(msg: Any) {
        System.err.println("${delegate.name}: $msg")
    }

    fun error(msg: Any, e: Throwable) {
        error(msg)
        traceback(e, verbose)
    }

    fun error(msg: Any, e: Throwable, level: AppVerbose) {
        error(msg)
        traceback(e, level)
    }

    fun die(msg: Any): Nothing {
        error(msg)
        exit(-1)
    }

    fun die(msg: Any, e: Throwable): Nothing {
        error(msg, e)
        exit(-1)
    }

    fun die(msg: Any, e: Throwable, level: AppVerbose): Nothing {
        error(msg, e, level)
        exit(-1)
    }

    fun traceback(e: Throwable, level: AppVerbose) {
        when (level) {
            AppVerbose.ECHO -> System.err.println("\t${e.message}")
            AppVerbose.TRACE -> e.printStackTrace()
            else -> Unit
        }
    }

    fun registerCleanup(action: Cleanup) {
        cleanups += action
    }

    fun removeCleanup(action: Cleanup) {
        cleanups -= action
    }

    fun initAppHome() = File(home).run { exists() || mkdirs() }

    fun resetAppHome() = File(home).deleteRecursively()
}
