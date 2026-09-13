package com.markedusduplicate.textresource

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes

/**
 * A piece of text that knows what it says without yet knowing how to say it: it names a string
 * resource (or carries a literal) and turns into a [String] only when something with [Resources] in
 * hand asks it to.
 *
 * That deferral is the whole point. Code that decides *what* the product says — a use case, a
 * service, anything off the UI thread of the composition — can hand a `TextResource` onward without
 * holding a [Context] to make a sentence, and the words stay in `strings.xml` where they can be read
 * top to bottom.
 *
 * Resolve it at the draw site: `asString()` inside a composable, or `asString(context)` from an
 * `Activity`, a `Service`, or anywhere else holding a [Context].
 *
 * ### Equality
 * Instances from the [raw] and [simple] factories are backed by private data classes, so they have
 * value equality and stable hash codes — safe as a `remember` key, in a `StateFlow`'s state, or in a
 * `Set`. An instance made through the SAM initializer is an anonymous implementation with
 * **reference** equality:
 *
 * ```
 * TextResource { resources -> "…" } != TextResource { resources -> "…" }
 * ```
 *
 * Prefer the factories whenever the value gets compared, deduplicated or cached.
 *
 * ---
 * Adapted from [TextResource](https://github.com/dkmarkell/textresource) by Derek Markell, MIT
 * licensed — see `THIRD_PARTY_NOTICES.md`. Trimmed to the two factories this app uses (upstream also
 * has `plural`), `resolveString` renamed to `asString`, and resolution moved from [Context] to
 * [Resources] to match the seam Compose's own `stringResource()` reads.
 */
fun interface TextResource {

    /** Turns this into displayable text for [resources]' current configuration. */
    fun asString(resources: Resources): String

    companion object {

        /**
         * A literal that is the same in every locale — a name, a symbol, something already read
         * off the wire.
         */
        fun raw(text: String): TextResource = Raw(text)

        /**
         * A string resource, with optional formatting arguments for its placeholders.
         *
         * ```
         * TextResource.simple(R.string.voice_catchphrase)
         * TextResource.simple(R.string.voice_too_thin, MIN_WORDS_TO_DETECT)
         * ```
         */
        fun simple(@StringRes resId: Int, vararg args: Any): TextResource =
            Simple(resId = resId, args = args.toList())
    }
}

/** Resolves against a [Context]'s resources, for callers holding one rather than [Resources]. */
fun TextResource.asString(context: Context): String = asString(context.resources)

/** Backs [TextResource.raw]. A data class so two raws with the same text are equal. */
private data class Raw(val text: String) : TextResource {
    override fun asString(resources: Resources): String = text
}

/**
 * Backs [TextResource.simple].
 *
 * [args] is a copy of the caller's vararg array rather than the array itself, so equality is by
 * value and a caller that mutates its array afterwards cannot change what this says.
 *
 * With no args it takes the plain [Resources.getString] overload rather than passing an empty array
 * to the formatting one, which is what Compose's own `stringResource()` does — and what keeps a
 * literal `%` in an unformatted string from blowing up in `String.format`.
 */
private data class Simple(@StringRes val resId: Int, val args: List<Any>) : TextResource {
    override fun asString(resources: Resources): String =
        if (args.isEmpty()) {
            resources.getString(resId)
        } else {
            resources.getString(resId, *args.toTypedArray())
        }
}
