package com.example.sudokusolver.domain

object SudokuSolver {
    fun solve(board: SudokuBoard): SudokuBoard? {
        if (SudokuValidator.findConflicts(board).isNotEmpty()) return null
        val workingBoard = board.toIntArray()
        return if (solveNextCell(workingBoard)) SudokuBoard(workingBoard) else null
    }

    private fun solveNextCell(board: IntArray): Boolean {
        val index = board.indexOfFirst { value -> value == 0 }
        if (index == -1) return true

        val row = index / 9
        val column = index % 9

        for (number in 1..9) {
            if (canPlaceNumber(board, row, column, number)) {
                board[index] = number
                if (solveNextCell(board)) return true
                board[index] = 0
            }
        }

        return false
    }

    private fun canPlaceNumber(board: IntArray, row: Int, column: Int, number: Int): Boolean {
        for (offset in 0 until 9) {
            if (board[row * 9 + offset] == number) return false
            if (board[offset * 9 + column] == number) return false
        }

        val boxStartRow = row / 3 * 3
        val boxStartColumn = column / 3 * 3
        for (boxRow in boxStartRow until boxStartRow + 3) {
            for (boxColumn in boxStartColumn until boxStartColumn + 3) {
                if (board[boxRow * 9 + boxColumn] == number) return false
            }
        }

        return true
    }
}
