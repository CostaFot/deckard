package com.markedusduplicate.deckard.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.markedusduplicate.deckard.mascot.DeckardOverlayService

/**
 * Invisible share target. Receives text shared from any app (`ACTION_SEND` / `text/plain`) and hands
 * it to [DeckardOverlayService], which judges it and pops the mascot up to "speak" the verdict over
 * whatever's on screen — no overlay gesture and no screen read required, only the draw-over-apps
 * permission. Forwards and [finish]es immediately; it never shows UI (translucent theme).
 */
class ShareTextActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent
            ?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)
            ?.trim()

        when {
            text.isNullOrBlank() ->
                toast("Nothing for Deckard to judge")

            !Settings.canDrawOverlays(this) -> {
                toast("Let Deckard draw over apps first")
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }

            else -> DeckardOverlayService.detectText(this, text)
        }

        finish()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
