package com.example.sudokusolver.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SudokuValidatorTest {
    @Test
    fun `valid rows have no conflicts`() {
        val board = IntArray(81) { index -> index % 9 + 1 }

        val conflicts = SudokuValidator.findRowConflicts(board)

        assertEquals(emptySet<Int>(), conflicts)
    }

    @Test
    fun `duplicate values in a row are conflicts`() {
        val board = IntArray(81)
        board[18] = 5
        board[26] = 5
        val originalBoard = board.copyOf()

        val conflicts = SudokuValidator.findRowConflicts(board)

        assertEquals(setOf(18, 26), conflicts)
        assertArrayEquals(originalBoard, board)
    }

    @Test
    fun `empty cells are ignored`() {
        val board = IntArray(81)
        assertEquals(emptySet<Int>(), SudokuValidator.findRowConflicts(board))

        board[0] = 5
        board[9] = 5

        assertEquals(emptySet<Int>(), SudokuValidator.findRowConflicts(board))
    }

    @Test
    fun `all occurrences of a repeated value are conflicts`() {
        val board = IntArray(81)
        board[72] = 7
        board[75] = 7
        board[80] = 7

        val conflicts = SudokuValidator.findRowConflicts(board)

        assertEquals(setOf(72, 75, 80), conflicts)
    }

    @Test
    fun `conflicts from different rows are collected`() {
        val board = IntArray(81)
        board[0] = 4
        board[8] = 4
        board[36] = 2
        board[40] = 2
        board[80] = 9

        val conflicts = SudokuValidator.findRowConflicts(board)

        assertEquals(setOf(0, 8, 36, 40), conflicts)
    }
}
