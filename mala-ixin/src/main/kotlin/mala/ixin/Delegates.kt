package mala.ixin

import mala.core.App
import mala.core.AppDelegate
import javax.swing.SwingUtilities

abstract class IDelegate : AppDelegate, CommandHandler {
    open fun onInit() {}

    open fun onReady() {}

    final override fun run() {
        SwingUtilities.invokeLater {
            onInit()
            onReady()
        }
    }

    val proxy = CommandDispatcher()

    override fun performed(command: String): Boolean {
        if (!proxy.performed(command)) {
            App.error("no handler for command: $command")
            return false
        }
        return true
    }
}
