package com.example.sudokusolver.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuSolverTest {
    @Test
    fun `known puzzle is solved with the expected values`() {
        val board = examplePuzzle()

        val solved = SudokuSolver.solve(board)

        assertTrue(solved)
        assertArrayEquals(expectedSolution(), board)
    }

    @Test
    fun `already solved board is accepted unchanged`() {
        val board = expectedSolution()
        val originalBoard = board.copyOf()

        val solved = SudokuSolver.solve(board)

        assertTrue(solved)
        assertArrayEquals(originalBoard, board)
    }

    @Test
    fun `conflicting clues are rejected without changing the board`() {
        val board = examplePuzzle()
        board[2] = 5
        val originalBoard = board.copyOf()

        val solved = SudokuSolver.solve(board)

        assertFalse(solved)
        assertArrayEquals(originalBoard, board)
    }

    @Test
    fun `unsolvable board restores all attempted values`() {
        val board = examplePuzzle()
        board[2] = 1
        val originalBoard = board.copyOf()
        assertTrue(SudokuValidator.findConflicts(board).isEmpty())

        val solved = SudokuSolver.solve(board)

        assertFalse(solved)
        assertArrayEquals(originalBoard, board)
    }

    @Test
    fun `completed board with conflicts is rejected`() {
        val board = expectedSolution()
        board[0] = 3
        val originalBoard = board.copyOf()

        val solved = SudokuSolver.solve(board)

        assertFalse(solved)
        assertArrayEquals(originalBoard, board)
    }

    private fun examplePuzzle(): IntArray = intArrayOf(
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

    private fun expectedSolution(): IntArray = intArrayOf(
        5, 3, 4, 6, 7, 8, 9, 1, 2,
        6, 7, 2, 1, 9, 5, 3, 4, 8,
        1, 9, 8, 3, 4, 2, 5, 6, 7,
        8, 5, 9, 7, 6, 1, 4, 2, 3,
        4, 2, 6, 8, 5, 3, 7, 9, 1,
        7, 1, 3, 9, 2, 4, 8, 5, 6,
        9, 6, 1, 5, 3, 7, 2, 8, 4,
        2, 8, 7, 4, 1, 9, 6, 3, 5,
        3, 4, 5, 2, 8, 6, 1, 7, 9
    )
}
