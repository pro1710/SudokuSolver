package com.example.sudokusolver.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuBoardTest {
    @Test
    fun `default board has 81 empty cells`() {
        val board = SudokuBoard()

        assertArrayEquals(IntArray(81), board.toIntArray())
        assertEquals(0, board.clueCount)
        assertFalse(board.isComplete)
    }

    @Test
    fun `cells can be read by index and position`() {
        val values = IntArray(81)
        values[0] = 5
        values[23] = 7
        values[80] = 9
        val board = SudokuBoard(values)

        assertEquals(5, board[0])
        assertEquals(7, board[23])
        assertEquals(7, board[CellPosition(2, 5)])
        assertEquals(9, board[CellPosition(8, 8)])
        assertEquals(0, board[24])
        assertEquals(3, board.clueCount)
    }

    @Test
    fun `boards with the wrong number of cells are rejected`() {
        for (size in listOf(0, 80, 82)) {
            assertThrows(IllegalArgumentException::class.java) { SudokuBoard(IntArray(size)) }
        }
    }

    @Test
    fun `initial values outside zero through nine are rejected`() {
        for (value in listOf(-1, 10)) {
            val values = IntArray(81)
            values[80] = value
            assertThrows(IllegalArgumentException::class.java) { SudokuBoard(values) }
        }
    }

    @Test
    fun `changing the constructor array does not change the board`() {
        val values = IntArray(81)
        values[23] = 7
        val board = SudokuBoard(values)

        values[23] = 9
        values[0] = 5

        assertEquals(7, board[23])
        assertEquals(0, board[0])
        assertEquals(1, board.clueCount)
    }

    @Test
    fun `changing an exported array does not change the board`() {
        val board = SudokuBoard().withValue(23, 7)
        val exported = board.toIntArray()

        exported[23] = 0

        assertEquals(7, board[23])
        assertEquals(7, board.toIntArray()[23])
        assertEquals(1, board.clueCount)
    }

    @Test
    fun `updating an index leaves the original board unchanged`() {
        val original = SudokuBoard().withValue(0, 5)
        val expected = original.toIntArray()

        val updated = original.withValue(23, 7)

        assertArrayEquals(expected, original.toIntArray())
        expected[23] = 7
        assertArrayEquals(expected, updated.toIntArray())
        assertEquals(1, original.clueCount)
        assertEquals(2, updated.clueCount)
    }

    @Test
    fun `updating a position changes the corresponding cell`() {
        val original = SudokuBoard()

        val updated = original.withValue(CellPosition(2, 5), 9)

        assertEquals(0, original[23])
        assertEquals(9, updated[23])
        assertEquals(1, updated.clueCount)
    }

    @Test
    fun `zero clears a value without changing the previous board`() {
        val original = SudokuBoard().withValue(23, 7)

        val cleared = original.withValue(CellPosition(2, 5), 0)

        assertEquals(7, original[23])
        assertArrayEquals(IntArray(81), cleared.toIntArray())
        assertEquals(0, cleared.clueCount)
    }

    @Test
    fun `every Sudoku digit can be stored`() {
        for (digit in 1..9) {
            val board = SudokuBoard().withValue(0, digit)
            assertEquals(digit, board[0])
        }
    }

    @Test
    fun `invalid lookup and update indices are rejected`() {
        val board = SudokuBoard()
        for (index in listOf(-1, 81)) {
            assertThrows(IllegalArgumentException::class.java) { board[index] }
            assertThrows(IllegalArgumentException::class.java) { board.withValue(index, 5) }
        }
    }

    @Test
    fun `invalid update values are rejected without changing the board`() {
        val board = SudokuBoard()
        for (value in listOf(-1, 10)) {
            assertThrows(IllegalArgumentException::class.java) { board.withValue(23, value) }
            assertThrows(IllegalArgumentException::class.java) {
                board.withValue(CellPosition(2, 5), value)
            }
        }
        assertArrayEquals(IntArray(81), board.toIntArray())
    }

    @Test
    fun `complete means all cells are filled even if digits conflict`() {
        val board = SudokuBoard(IntArray(81) { 1 })

        assertTrue(board.isComplete)
        assertEquals(81, board.clueCount)
    }

    @Test
    fun `a board with one empty cell is incomplete`() {
        val board = SudokuBoard(IntArray(81) { 1 }).withValue(80, 0)

        assertFalse(board.isComplete)
        assertEquals(80, board.clueCount)
    }
}
