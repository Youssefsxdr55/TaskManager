package com.joe.taskmanager.ui.navigation

import androidx.lifecycle.SavedStateHandle

/** Reads a navigation ID whether Navigation supplied its typed Long or a String route value. */
fun SavedStateHandle.longArg(name: String): Long? = when (val value = get<Any?>(name)) {
    is Number -> value.toLong()
    is String -> value.toLongOrNull()
    else -> null
}
