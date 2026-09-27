package com.example.sudokusolver.domain

object SudokuValidator {
    fun findRowConflicts(board: IntArray): Set<Int> {
        val conflicts = mutableSetOf<Int>()

        for (row in 0 until 9) {
            for (column in 0 until 9) {
                val index = row * 9 + column
                val value = board[index]
                if (value == 0) continue

                for (otherColumn in column + 1 until 9) {
                    val otherIndex = row * 9 + otherColumn
                    if (board[otherIndex] == value) {
                        conflicts.add(index)
                        conflicts.add(otherIndex)
                    }
                }
            }
        }

        return conflicts
    }
}
