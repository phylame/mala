import mala.core.App
import mala.ixin.*

object MyApp : IDelegate() {
    val dispatcher = CommandDispatcher()

    override fun onStart() {
        super.onStart()
        App.translator = App.resourceManager.linguistFor("i18n/app")
        dispatcher.register(this)
    }

    override fun onInit() {
        val form = Form()
        dispatcher.register(form)
        form.isVisible = true
    }

    override val name = "MyApp"

    override val version = "0.1"

    @Command
    fun exitApp() {
        App.exit()
    }
}

class Form : IForm(MyApp.name) {
    init {
        IxIn.isAntiAliasing = true
        IxIn.updateSwingTheme(IxIn.swingTheme)
        IxIn.updateGlobalFont(IxIn.globalFont)
        val d = App.resourceManager.designerFor("ui/app.json")
        init(d!!, MyApp.dispatcher, App, App.resourceManager)
        statusText = "Ready"
        pack()
    }
}

fun main(args: Array<String>) {
    App.run(MyApp, args)
}
