import mala.core.App
import mala.ixin.IDelegate

object MyUI : IDelegate() {
    override fun initUI() {
        super.initUI()
    }

    override val name = "myUI"

    override val version = "0.1"
    
    
}

fun main(args: Array<String>) {
    App.run(MyUI, args)
}