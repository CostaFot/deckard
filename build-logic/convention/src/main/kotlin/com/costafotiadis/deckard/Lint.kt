package com.costafotiadis.deckard

import com.android.build.api.dsl.Lint

/**
 * One lint configuration for the app and every library: every report format, and the libraries a
 * module depends on checked along with it.
 */
internal fun Lint.configureDeckardLint() {
    xmlReport = true
    htmlReport = true
    sarifReport = true
    checkDependencies = true
}
