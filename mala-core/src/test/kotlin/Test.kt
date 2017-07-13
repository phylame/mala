import jclp.setting.Settings
import jclp.util.Linguist
import mala.core.*

object MySettings : Settings(App.pathOf("app")) {
    var name by mapped("N/A")
    var age by mapped(-1)
    var sex by mapped(true, "sex_1")
}

object MyDelegate : AppDelegate {

    override val name: String = "testApp"

    override val version: String = "1.0"

    val rm = ResourceManager("!")

    override fun onStart() {
        App.verbose = AppVerbose.TRACE
        App.setTranslator(Linguist("x"))
        println(MySettings.age)
        println(MySettings.values.asSequence().toList())
        MySettings.age = 3456
        println(MySettings.sex)
        MySettings.sex = false
        println(MySettings.values.asSequence().toList())
    }

    override fun run() {
        App.exit()
    }
}

fun main(args: Array<String>) {
    App.run(MyDelegate, args)
}
