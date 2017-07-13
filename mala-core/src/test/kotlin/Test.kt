import jclp.io.IOUtils
import jclp.setting.Settings
import jclp.util.Linguist
import mala.core.App
import mala.core.AppDelegate
import mala.core.AppVerbose
import mala.core.ResourceManager

object MyDelegate : AppDelegate {
    override fun onStop() {
        println(App.resetAppHome())
    }

    override val name: String = "testApp"

    override val version: String = "1.0"

    val rm = ResourceManager("!")

    override fun onStart() {
        App.verbose = AppVerbose.TRACE
        App.setTranslator(Linguist("x"))
        println(App.initAppHome())

        val settings = Settings(App.pathOf("settings/app"))
        App.registerCleanup(settings::sync)
        println(settings["your_name"])
        println(settings.isEnable("app.verbose"))
        println(IOUtils.resourceFor("!x.properties"))
        println(rm.linguistFor("x").optTr("app.name", ""))
    }

    override fun run() {
        App.exit()
    }
}

fun main(args: Array<String>) {
    App.run(MyDelegate, args)
}
