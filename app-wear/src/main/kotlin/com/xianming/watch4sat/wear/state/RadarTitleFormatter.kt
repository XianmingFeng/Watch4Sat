package com.xianming.watch4sat.wear.state

object RadarTitleFormatter {
    private const val SafeLineLength = 12

    fun format(title: String): String {
        val trimmed = title.trim()
        if (trimmed.length <= 16) return trimmed

        val parenthesisIndex = trimmed.indexOf(" (")
        if (parenthesisIndex in 1 until trimmed.lastIndex) {
            return trimmed.substring(0, parenthesisIndex) + "\n" + trimmed.substring(parenthesisIndex + 1)
        }

        if (trimmed.contains('\n')) return trimmed

        val breakIndex = SafeLineLength.coerceAtMost(trimmed.length - 1)
        return trimmed.substring(0, breakIndex) + "\n" + trimmed.substring(breakIndex)
    }
}
