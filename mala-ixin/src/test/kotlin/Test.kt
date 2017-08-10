import mala.core.App
import mala.ixin.*
import java.awt.BorderLayout
import javax.swing.Action
import javax.swing.JButton
import javax.swing.JLabel

object MyApp : IDelegate() {
    val dispatcher = CommandDispatcher()

    override fun onStart() {
        super.onStart()
        App.translator = App.resourceManager.linguistFor("i18n/app")
        dispatcher.addProxy(this)
    }

    override fun initUI() {
        val form = Form()
        dispatcher.addProxy(form)
        form.isVisible = true
    }

    override val name = "MyApp"

    override val version = "0.1"

    @Command("exit-app")
    fun exitApp() {
        App.exit()
    }
}

class Form : IForm(MyApp.name) {
    init {
        IxIn.isAntiAliasing = true
        IxIn.updateSwingTheme(IxIn.swingTheme)
        IxIn.updateGlobalFont(IxIn.globalFont)
        defaultCloseOperation = EXIT_ON_CLOSE
        val action = DispatcherAction("exit-app", MyApp.dispatcher)
        action[Action.SHORT_DESCRIPTION]=IxIn
        println(action)
        contentPane.add(JLabel("Test App by WP"))
        contentPane.add(JButton(action), BorderLayout.PAGE_END)
        pack()
    }
}

fun main(args: Array<String>) {
    App.run(MyApp, args)
}
