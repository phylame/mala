import mala.core.App
import mala.core.AppDelegate
import mala.core.Plugin

object Test : AppDelegate, Plugin {
    override val name = "Test"

    override val version = "1.0"

    override fun run() {
        App.die("xyz")
    }

    override fun init() {
        println("init plugin")
    }

    override fun destroy() {
        println("destroy plugin")
    }
}

fun main(args: Array<String>) {
    App.run(Test, args)
}