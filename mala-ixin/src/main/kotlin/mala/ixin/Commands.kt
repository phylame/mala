package mala.ixin

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*
import kotlin.collections.HashMap

interface CommandHandler {
    fun performed(command: String): Boolean
}

annotation class Command(val name: String = "")

class CommandDispatcher(proxies: Array<out Any> = emptyArray()) : CommandHandler {
    private val handlers = LinkedList<CommandHandler>()
    private val invocations = HashMap<String, Invocation>()

    init {
        register(proxies)
    }

    fun reset() {
        handlers.clear()
        invocations.clear()
    }

    fun register(vararg proxies: Any) {
        for (proxy in proxies) {
            proxy.javaClass.methods.filter {
                !Modifier.isStatic(it.modifiers) && !Modifier.isAbstract(it.modifiers) && it.parameterTypes.isEmpty()
            }.forEach {
                val command = it.getAnnotation(Command::class.java)
                if (command != null) {
                    val invocation = Invocation(proxy, it)
                    invocations.put(if (command.name.isNotEmpty()) command.name else it.name, invocation)
                }
            }
            if (proxy is CommandHandler) {
                handlers += proxy
                continue
            }
        }
    }

    override fun performed(command: String): Boolean {
        val invocation = invocations[command]
        if (invocation != null) {
            return invocation.invoke(command)
        }
        for (handler in handlers) {
            if (handler.performed(command)) {
                invocations[command] = Invocation(handler)
                return true
            }
        }
        return false
    }

    override fun toString() = "CommandDispatcher(handlers=$handlers, invocations=$invocations)"

    private data class Invocation(val proxy: Any, val method: Method? = null) {
        fun invoke(command: String) = if (method != null) {
            method.invoke(proxy)
            true
        } else (proxy as? CommandHandler)?.performed(command) == true
    }
}
