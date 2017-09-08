package mala.core

infix fun String?.or(lazyString: () -> String): String = if (isNullOrEmpty()) lazyString() else this!!