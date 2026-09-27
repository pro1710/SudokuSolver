package com.example.sudokusolver.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SudokuValidatorTest {
    @Test
    fun `valid board has no conflicts`() {
        val board = SudokuBoard(
            intArrayOf(
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
        )

        val conflicts = SudokuValidator.findConflicts(board)

        assertEquals(emptySet<Int>(), conflicts)
    }

    @Test
    fun `duplicate values in a row are conflicts`() {
        val board = SudokuBoard()
            .withValue(18, 5)
            .withValue(26, 5)
        val originalBoard = board.toIntArray()

        val conflicts = SudokuValidator.findConflicts(board)

        assertEquals(setOf(18, 26), conflicts)
        assertArrayEquals(originalBoard, board.toIntArray())
    }

    @Test
    fun `duplicate values in a column are conflicts`() {
        val board = SudokuBoard()
            .withValue(8, 6)
            .withValue(80, 6)

        val conflicts = SudokuValidator.findConflicts(board)

        assertEquals(setOf(8, 80), conflicts)
    }

    @Test
    fun `duplicate values in a box are conflicts`() {
        val board = SudokuBoard()
            .withValue(60, 8)
            .withValue(80, 8)

        val conflicts = SudokuValidator.findConflicts(board)

        assertEquals(setOf(60, 80), conflicts)
    }

    @Test
    fun `empty cells are ignored`() {
        val board = SudokuBoard()
        assertEquals(emptySet<Int>(), SudokuValidator.findConflicts(board))

        val updated = board.withValue(0, 5).withValue(1, 3)

        assertEquals(emptySet<Int>(), SudokuValidator.findConflicts(updated))
    }

    @Test
    fun `same value in unrelated cells is allowed`() {
        val board = SudokuBoard()
            .withValue(0, 5)
            .withValue(40, 5)

        val conflicts = SudokuValidator.findConflicts(board)

        assertEquals(emptySet<Int>(), conflicts)
    }

    @Test
    fun `all occurrences of a repeated value are conflicts`() {
        val board = SudokuBoard()
            .withValue(72, 7)
            .withValue(75, 7)
            .withValue(80, 7)

        val conflicts = SudokuValidator.findConflicts(board)

        assertEquals(setOf(72, 75, 80), conflicts)
    }

    @Test
    fun `conflicts from rows columns and boxes are collected`() {
        val board = SudokuBoard()
            .withValue(0, 4)
            .withValue(8, 4)
            .withValue(9, 2)
            .withValue(63, 2)
            .withValue(60, 7)
            .withValue(70, 7)
            .withValue(40, 9)

        val conflicts = SudokuValidator.findConflicts(board)

        assertEquals(setOf(0, 8, 9, 63, 60, 70), conflicts)
    }
}
