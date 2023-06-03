package common

import java.util.*

fun <T> T.printIt() {
    println(this)
}

fun String.capitalizeIt(): String {
    return replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(
            Locale.getDefault()
        ) else it.toString()
    }
}

fun String?.ifNotEmpty(block: () -> String): String {
    return if (!isNullOrEmpty()) block() else ""
}