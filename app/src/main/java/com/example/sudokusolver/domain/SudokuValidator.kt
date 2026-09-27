package com.example.sudokusolver.domain

object SudokuValidator {
    fun findConflicts(board: IntArray): Set<Int> {
        val conflicts = mutableSetOf<Int>()

        for (row in 0 until 9) {
            val indices = (0 until 9).map { column -> row * 9 + column }
            conflicts.addAll(findConflictsInGroup(board, indices))
        }

        for (column in 0 until 9) {
            val indices = (0 until 9).map { row -> row * 9 + column }
            conflicts.addAll(findConflictsInGroup(board, indices))
        }

        for (boxRow in 0 until 3) {
            for (boxColumn in 0 until 3) {
                val indices = mutableListOf<Int>()
                for (rowOffset in 0 until 3) {
                    for (columnOffset in 0 until 3) {
                        val row = boxRow * 3 + rowOffset
                        val column = boxColumn * 3 + columnOffset
                        indices.add(row * 9 + column)
                    }
                }
                conflicts.addAll(findConflictsInGroup(board, indices))
            }
        }

        return conflicts
    }

    private fun findConflictsInGroup(board: IntArray, indices: List<Int>): Set<Int> {
        val conflicts = mutableSetOf<Int>()

        for (position in indices.indices) {
            val index = indices[position]
            val value = board[index]
            if (value == 0) continue

            for (otherPosition in position + 1 until indices.size) {
                val otherIndex = indices[otherPosition]
                if (board[otherIndex] == value) {
                    conflicts.add(index)
                    conflicts.add(otherIndex)
                }
            }
        }

        return conflicts
    }
}
