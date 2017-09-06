package mala.core

enum class AppState {
    DEFAULT,
    STARTING,
    RUNNING,
    STOPPING
}

interface AppDelegate : Runnable {
    val name: String

    val version: String

    fun onStart() {}

    fun onStop() {}
}

object App {
    var state = AppState.DEFAULT
        private set

    fun run(delegate: AppDelegate, args: Array<String>) {
        state = AppState.STARTING
        delegate.onStart()
        state = AppState.RUNNING
        delegate.run()
    }

    fun exit(code: Int = 0): Nothing {
        state = AppState.STOPPING
        System.exit(code)
        throw InternalError()
    }
}
