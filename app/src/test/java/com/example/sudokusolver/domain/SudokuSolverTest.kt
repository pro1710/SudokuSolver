package com.example.sudokusolver.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuSolverTest {
    @Test
    fun `known puzzle is solved with the expected values`() {
        val board = SudokuBoard(examplePuzzle())

        val solved = SudokuSolver.solve(board)

        assertNotNull(solved)
        assertArrayEquals(expectedSolution(), solved?.toIntArray())
    }

    @Test
    fun `successful solving does not modify the input board`() {
        val board = SudokuBoard(examplePuzzle())
        val originalValues = board.toIntArray()

        val solved = SudokuSolver.solve(board)

        assertNotNull(solved)
        assertArrayEquals(originalValues, board.toIntArray())
        assertArrayEquals(expectedSolution(), solved?.toIntArray())
    }

    @Test
    fun `already solved board is accepted unchanged`() {
        val board = SudokuBoard(expectedSolution())
        val originalValues = board.toIntArray()

        val solved = SudokuSolver.solve(board)

        assertNotNull(solved)
        assertArrayEquals(originalValues, solved?.toIntArray())
        assertArrayEquals(originalValues, board.toIntArray())
    }

    @Test
    fun `conflicting clues are rejected without changing the board`() {
        val board = SudokuBoard(examplePuzzle()).withValue(2, 5)
        val originalValues = board.toIntArray()

        val solved = SudokuSolver.solve(board)

        assertNull(solved)
        assertArrayEquals(originalValues, board.toIntArray())
    }

    @Test
    fun `unsolvable board returns null without changing the input`() {
        val board = SudokuBoard(examplePuzzle()).withValue(2, 1)
        val originalValues = board.toIntArray()
        assertTrue(SudokuValidator.findConflicts(board).isEmpty())

        val solved = SudokuSolver.solve(board)

        assertNull(solved)
        assertArrayEquals(originalValues, board.toIntArray())
    }

    @Test
    fun `completed board with conflicts is rejected`() {
        val board = SudokuBoard(expectedSolution()).withValue(0, 3)
        val originalValues = board.toIntArray()

        val solved = SudokuSolver.solve(board)

        assertNull(solved)
        assertArrayEquals(originalValues, board.toIntArray())
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
