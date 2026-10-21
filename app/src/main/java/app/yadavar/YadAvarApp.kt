package app.yadavar

import android.app.Application
import app.yadavar.reminder.NotificationHelper

class YadAvarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}
