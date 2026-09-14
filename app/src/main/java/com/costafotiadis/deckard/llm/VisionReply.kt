package com.costafotiadis.deckard.llm

/**
 * What the vision model said about a screenshot: the raw reply, or why there is none. The model
 * names the condition in its own terms and nothing more; the screen read maps it onto a
 * [com.costafotiadis.deckard.slop.ScreenReadFailure] and Deckard words it.
 */
sealed interface VisionReply {
    data class Text(val value: String) : VisionReply
    data class Failed(val reason: VisionFailure) : VisionReply
}

/** Why a [VisionModel] gave no reply. */
enum class VisionFailure {
    /** Not loaded, not present, or not supported on this device. */
    NotReady,

    /** The model serves only the app in front of the screen, and Deckard could not get there. */
    NotInFront,

    /** The model read the screen and declined to repeat what was on it. */
    Refused,

    /** The model's allowance for this app is spent for the day. */
    OutOfQuota,

    /** Inference ran and produced nothing. */
    Failed,
}
