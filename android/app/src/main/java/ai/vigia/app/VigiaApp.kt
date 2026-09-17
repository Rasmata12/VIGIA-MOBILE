package ai.vigia.app

import android.app.Application
import ai.vigia.app.notif.Notifier

class VigiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        Notifier.createChannels(this)
    }
}
