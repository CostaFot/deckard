package com.costafotiadis.common

interface FlagProvider {
    val isDebugEnabled: Boolean
    val isRunningUiTest: Boolean
}
