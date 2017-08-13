package mala.ixin

import mala.core.App
import java.lang.reflect.Method
import java.lang.reflect.Modifier

interface CommandListener {
    fun performed(command: String)
}

annotation class Command(val name: String = "")

class CommandDispatcher(proxies: Array<out Any> = emptyArray()) : CommandListener {
    private val invocations = HashMap<String, Invocation>()

    init {
        proxies.forEach(this::addProxy)
    }

    override fun performed(command: String) {
        invocations[command]?.invoke() ?: App.error("No handler for command: $command")
    }

    fun addProxies(vararg proxies: Any) {
        proxies.forEach(this::addProxy)
    }

    fun addProxy(proxy: Any) {
        proxy.javaClass.methods.filter {
            Modifier.isPublic(it.modifiers)
                    && !Modifier.isStatic(it.modifiers)
                    && !Modifier.isAbstract(it.modifiers)
                    && it.parameterTypes.isEmpty()
        }.forEach {
            val command = it.getAnnotation(Command::class.java)
            if (command != null) {
                val invocation = Invocation(proxy, it)
                invocations.put(if (command.name.isNotEmpty()) command.name else it.name, invocation)
            }
        }
    }

    data class Invocation(val proxy: Any, val method: Method) {
        fun invoke() {
            method.invoke(proxy)
        }
    }
}
