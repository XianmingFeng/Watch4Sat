package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.qth.QthInputValidator

object QthPickerPolicy {
    const val usesCompactNonScrollingPage: Boolean = true
    const val hidesSelectedValueAbovePicker: Boolean = true
    const val positionCount: Int = 6

    private val fieldLetters = ('A'..'R').toList()
    private val squareDigits = ('0'..'9').toList()
    private val subsquareLetters = ('A'..'X').toList()

    fun optionsForPosition(position: Int): List<Char> {
        return when (position) {
            0, 1 -> fieldLetters
            2, 3 -> squareDigits
            4, 5 -> subsquareLetters
            else -> error("QTH picker position must be 0..5.")
        }
    }

    fun initialQth(currentQth: String?, mapQth: String?): String {
        return listOfNotNull(currentQth, mapQth)
            .map { QthInputValidator.normalize(it).take(6) }
            .firstOrNull { isValid(it) }
            ?: "AA00AA"
    }

    fun indexesForQth(qth: String): IntArray {
        val normalized = QthInputValidator.normalize(qth).padEnd(6, 'A').take(6)
        return IntArray(positionCount) { position ->
            optionsForPosition(position).indexOf(normalized[position]).coerceAtLeast(0)
        }
    }

    fun qthForIndexes(indexes: IntArray): String {
        require(indexes.size == positionCount) { "QTH picker needs exactly 6 indexes." }
        return indexes.mapIndexed { position, index ->
            val options = optionsForPosition(position)
            options[index.coerceIn(0, options.lastIndex)]
        }.joinToString("")
    }

    fun isValid(qth: String): Boolean {
        return QthInputValidator.isValid(qth)
    }
}
