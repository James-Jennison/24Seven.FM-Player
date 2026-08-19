package com.codeframe78.twentyfourseven.player.playback

import android.content.Context
import android.view.Menu
import androidx.mediarouter.app.MediaRouteButton
import com.codeframe78.twentyfourseven.player.R
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity

class PlayerCastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(CastExpandedControlsActivity::class.java.name)
            .build()
        return CastOptions.Builder()
            .setReceiverApplicationId(context.getString(R.string.cast_receiver_application_id))
            .setCastMediaOptions(
                CastMediaOptions.Builder()
                    .setNotificationOptions(notificationOptions)
                    .setExpandedControllerActivityClassName(CastExpandedControlsActivity::class.java.name)
                    .build(),
            )
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}

class CastExpandedControlsActivity : ExpandedControllerActivity() {
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(com.codeframe78.twentyfourseven.player.R.menu.cast_expanded_controller, menu)
        CastButtonFactory.setUpMediaRouteButton(applicationContext, menu, com.codeframe78.twentyfourseven.player.R.id.media_route_menu_item)
        return true
    }
}

internal fun setUpCastRouteButton(context: Context, button: MediaRouteButton) {
    CastButtonFactory.setUpMediaRouteButton(context.applicationContext, button)
}
