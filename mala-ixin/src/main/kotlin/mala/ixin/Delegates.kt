package mala.ixin

import mala.core.AppDelegate
import javax.swing.SwingUtilities

abstract class IDelegate : AppDelegate {
    open fun initUI() {}

    final override fun run() {
        SwingUtilities.invokeLater(this::initUI)
    }
}