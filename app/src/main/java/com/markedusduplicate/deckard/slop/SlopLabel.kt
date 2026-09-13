package com.markedusduplicate.deckard.slop

/**
 * What a passage is, in the detector's three-way terms: written by a machine, written by a person
 * with a machine's help, or written by a person.
 *
 * The middle value is the whole point — Pangram reports three shares that sum to the passage, and
 * collapsing them to a boolean early stamps a mixture as if it were wholly machine-written.
 */
enum class SlopLabel {
    AI,
    ASSISTED,
    HUMAN,
}
