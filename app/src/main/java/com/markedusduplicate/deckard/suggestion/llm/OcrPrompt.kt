package com.markedusduplicate.deckard.suggestion.llm

import com.markedusduplicate.deckard.suggestion.llm.OcrPrompt.extractMainContent
import com.markedusduplicate.deckard.suggestion.llm.OcrPrompt.transcribe


/**
 * Prompts for reading a screenshot with the on-device vision model, plus cleanup of the answer.
 * [transcribe] dumps all the text (OCR fallback); [extractMainContent] asks the model to isolate
 * the single main post out of the on-screen noise. Pure (no LiteRT types) so it's unit-testable.
 */
object OcrPrompt {

    fun transcribe(): String = buildString {
        append("This is a screenshot of my phone.\n")
        append("Transcribe all the readable text, in reading order.\n")
        append("Ignore the floating mascot, overlays, the keyboard, and the status / navigation bars.\n")
        append("Output only the transcribed text — no commentary, no labels, no explanation.")
    }

    /**
     * Asks the model to pick the one main post/article out of the screenshot and reproduce its
     * body **verbatim**. Verbatim is the point: the text feeds an AI-slop detector, so any
     * rewriting would bias the verdict toward "AI". Empty output is allowed and handled upstream.
     */
    fun extractMainContent(): String = buildString {
        append("This is a screenshot of a phone showing a social-media feed or article.\n")
        append("Exactly one main piece of writing is what the user is reading — a post, ")
        append("article, comment, or message. Find it and copy out its body text, word for word.\n")
        append("\n")
        append("Rules:\n")
        append("- Reproduce the main text exactly as written. Do not summarize, paraphrase, ")
        append("shorten, correct, translate, or add anything of your own.\n")
        append("- Include only the body of that one post or article.\n")
        append("- Leave everything else out: the author's name, @handle, and timestamp; ")
        append("buttons, tabs, and menu labels; like / comment / share / repost counts; ")
        append("\"Suggested for you\", \"Promoted\", and ads; other posts; the navigation and ")
        append("status bars; the keyboard; and any floating mascot or overlay.\n")
        append("- If more than one post is visible, pick the largest one closest to the ")
        append("centre of the screen.\n")
        append("- Output only that body text — no quotation marks, no labels, no notes. ")
        append("If there is no real body of writing on screen, output nothing.")
    }

    fun clean(raw: String): String = raw.trim().trim('"').trim()
}
