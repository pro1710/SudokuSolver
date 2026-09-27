package com.example.sudokusolver.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SudokuValidatorTest {
    @Test
    fun `valid board has no conflicts`() {
        val board = intArrayOf(
            5, 3, 0, 0, 7, 0, 0, 0, 0,
            6, 0, 0, 1, 9, 5, 0, 0, 0,
            0, 9, 8, 0, 0, 0, 0, 6, 0,
            8, 0, 0, 0, 6, 0, 0, 0, 3,
            4, 0, 0, 8, 0, 3, 0, 0, 1,
            7, 0, 0, 0, 2, 0, 0, 0, 6,
            0, 6, 0, 0, 0, 0, 2, 8, 0,
            0, 0, 0, 4, 1, 9, 0, 0, 5,
            0, 0, 0, 0, 8, 0, 0, 7, 9
        )

        val conflicts = SudokuValidator.findConflicts(board)

        assertEquals(emptySet<Int>(), conflicts)
    }

    @Test
    fun `duplicate values in a row are conflicts`() {
        val board = IntArray(81)
        board[18] = 5
        board[26] = 5
        val originalBoard = board.copyOf()

        val conflicts = SudokuValidator.findConflicts(board)

        assertEquals(setOf(18, 26), conflicts)
        assertArrayEquals(originalBoard, board)
    }

    @Test
    fun `duplicate values in a column are conflicts`() {
        val board = IntArray(81)
        board[8] = 6
        board[80] = 6

        val conflicts = SudokuValidator.findConflicts(board)

        assertEquals(setOf(8, 80), conflicts)
    }

    @Test
    fun `duplicate values in a box are conflicts`() {
        val board = IntArray(81)
        board[60] = 8
        board[80] = 8

        val conflicts = SudokuValidator.findConflicts(board)

        assertEquals(setOf(60, 80), conflicts)
    }

    @Test
    fun `empty cells are ignored`() {
        val board = IntArray(81)
        assertEquals(emptySet<Int>(), SudokuValidator.findConflicts(board))

        board[0] = 5
        board[1] = 3

        assertEquals(emptySet<Int>(), SudokuValidator.findConflicts(board))
    }

    @Test
    fun `same value in unrelated cells is allowed`() {
        val board = IntArray(81)
        board[0] = 5
        board[40] = 5

        val conflicts = SudokuValidator.findConflicts(board)

        assertEquals(emptySet<Int>(), conflicts)
    }

    @Test
    fun `all occurrences of a repeated value are conflicts`() {
        val board = IntArray(81)
        board[72] = 7
        board[75] = 7
        board[80] = 7

        val conflicts = SudokuValidator.findConflicts(board)

        assertEquals(setOf(72, 75, 80), conflicts)
    }

    @Test
    fun `conflicts from rows columns and boxes are collected`() {
        val board = IntArray(81)
        board[0] = 4
        board[8] = 4
        board[9] = 2
        board[63] = 2
        board[60] = 7
        board[70] = 7
        board[40] = 9

        val conflicts = SudokuValidator.findConflicts(board)

        assertEquals(setOf(0, 8, 9, 63, 60, 70), conflicts)
    }
}
