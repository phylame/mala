import jclp.setting.Settings
import jclp.util.Linguist
import mala.core.App
import mala.core.AppDelegate
import mala.core.AppVerbose
import mala.core.ResourceManager

object MyDelegate : AppDelegate {
    override val name: String = "testApp"

    override val version: String = "1.0"

    val rm = ResourceManager("!")

    override fun onStart() {
        App.verbose = AppVerbose.TRACE
        App.setTranslator(Linguist("x"))
        App.initAppHome()

        val settings = Settings(App.pathOf("settings/app"))
        App.registerCleanup(settings::sync)
        println(settings["your_name"])
        println(settings.isEnable("app.verbose"))
        println(rm.linguistFor("x").tr("app.name"))
    }

    override fun run() {
        App.exit()
    }
}

fun main(args: Array<String>) {
    App.run(MyDelegate, args)
}
