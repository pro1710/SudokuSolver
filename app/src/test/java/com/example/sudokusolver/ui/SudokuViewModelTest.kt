package com.example.sudokusolver.ui

import com.example.sudokusolver.domain.SudokuValidator
import com.example.sudokusolver.util.FakeLogger
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuViewModelTest {
    private val logger = FakeLogger()
    private val viewModel = SudokuViewModel(logger)

    @Test
    fun `cell is selected without changing the board`() {
        val before = viewModel.uiState.board.toIntArray()

        viewModel.selectCell(23)

        assertEquals(23, viewModel.uiState.selectedCell)
        assertArrayEquals(before, viewModel.uiState.board.toIntArray())
    }

    @Test
    fun `number is entered into selected cell`() {
        viewModel.selectCell(2)
        val previousState = viewModel.uiState

        viewModel.enterNumber(4)

        assertEquals(4, viewModel.uiState.board[2])
        assertEquals(4, viewModel.uiState.originalBoard[2])
        assertEquals(31, viewModel.uiState.originalBoard.clueCount)
        assertEquals(2, viewModel.uiState.selectedCell)
        assertEquals(0, previousState.board[2])
        assertEquals(0, previousState.originalBoard[2])
    }

    @Test
    fun `selected cell is cleared from board and clues`() {
        viewModel.selectCell(0)

        viewModel.clearCell()

        assertEquals(0, viewModel.uiState.board[0])
        assertEquals(0, viewModel.uiState.originalBoard[0])
        assertEquals(29, viewModel.uiState.originalBoard.clueCount)
        assertEquals(0, viewModel.uiState.selectedCell)
    }

    @Test
    fun `entry and clear without selection leave state unchanged and log warnings`() {
        val before = viewModel.uiState

        viewModel.enterNumber(4)
        viewModel.clearCell()

        assertEquals(before, viewModel.uiState)
        assertTrue(logger.entries.contains(FakeLogger.Entry(
            "WARNING", "SudokuViewModel", "Number ignored: number=4, no cell selected"
        )))
        assertTrue(logger.entries.contains(FakeLogger.Entry(
            "WARNING", "SudokuViewModel", "Clear ignored: no cell selected"
        )))
    }

    @Test
    fun `duplicate entry generates conflicting cell indices`() {
        viewModel.selectCell(2)

        viewModel.enterNumber(7)

        assertEquals(setOf(2, 4), viewModel.uiState.conflicts)
        assertEquals(7, viewModel.uiState.originalBoard[2])
    }

    @Test
    fun `clearing a duplicate removes conflicts`() {
        viewModel.selectCell(2)
        viewModel.enterNumber(7)

        viewModel.clearCell()

        assertTrue(viewModel.uiState.conflicts.isEmpty())
        assertEquals(0, viewModel.uiState.board[2])
    }

    @Test
    fun `conflicts prevent solving`() {
        viewModel.selectCell(2)
        viewModel.enterNumber(7)
        val before = viewModel.uiState

        viewModel.solve()

        assertEquals(before, viewModel.uiState)
        assertEquals(SudokuMode.EDIT, viewModel.uiState.mode)
        assertTrue(logger.entries.contains(FakeLogger.Entry(
            "WARNING", "SudokuViewModel", "Solve rejected: conflicts=[2, 4]"
        )))
        assertFalse(logger.entries.any { entry ->
            entry.message.startsWith("Starting Sudoku solver:")
        })
    }

    @Test
    fun `successful solve switches to solution and preserves accepted clues`() {
        viewModel.selectCell(2)
        viewModel.enterNumber(4)
        val clues = viewModel.uiState.originalBoard.toIntArray()

        viewModel.solve()

        assertEquals(SudokuMode.SOLUTION, viewModel.uiState.mode)
        assertNull(viewModel.uiState.selectedCell)
        assertTrue(viewModel.uiState.board.isComplete)
        assertTrue(SudokuValidator.findConflicts(viewModel.uiState.board).isEmpty())
        assertTrue(viewModel.uiState.conflicts.isEmpty())
        assertEquals(6, viewModel.uiState.board[3])
        assertEquals(0, viewModel.uiState.originalBoard[3])
        assertArrayEquals(clues, viewModel.uiState.originalBoard.toIntArray())
        assertTrue(logger.entries.contains(FakeLogger.Entry(
            "INFO", "SudokuViewModel", "Sudoku solved successfully"
        )))
    }

    @Test
    fun `solution mode ignores selection entry clear and repeated solve`() {
        viewModel.solve()
        val solution = viewModel.uiState

        viewModel.selectCell(2)
        viewModel.enterNumber(9)
        viewModel.clearCell()
        viewModel.solve()

        assertEquals(solution, viewModel.uiState)
    }

    @Test
    fun `unsolvable puzzle stays unchanged in edit mode`() {
        viewModel.selectCell(2)
        viewModel.enterNumber(1)
        assertTrue(viewModel.uiState.conflicts.isEmpty())
        val before = viewModel.uiState

        viewModel.solve()

        assertEquals(before, viewModel.uiState)
        assertEquals(SudokuMode.EDIT, viewModel.uiState.mode)
        assertTrue(logger.entries.contains(FakeLogger.Entry(
            "WARNING", "SudokuViewModel", "Sudoku has no solution"
        )))
    }

    @Test
    fun `edit clues restores manual clues and removes generated digits`() {
        viewModel.selectCell(2)
        viewModel.enterNumber(4)
        val clues = viewModel.uiState.originalBoard.toIntArray()
        viewModel.solve()

        viewModel.editClues()

        assertEquals(SudokuMode.EDIT, viewModel.uiState.mode)
        assertNull(viewModel.uiState.selectedCell)
        assertTrue(viewModel.uiState.conflicts.isEmpty())
        assertArrayEquals(clues, viewModel.uiState.board.toIntArray())
        assertArrayEquals(clues, viewModel.uiState.originalBoard.toIntArray())
        assertEquals(4, viewModel.uiState.board[2])
        assertEquals(0, viewModel.uiState.board[3])
    }

    @Test
    fun `reset restores initial puzzle from edit mode with conflicts`() {
        viewModel.selectCell(2)
        viewModel.enterNumber(7)

        viewModel.reset()

        assertInitialPuzzle()
    }

    @Test
    fun `reset restores initial puzzle from solution mode`() {
        viewModel.selectCell(2)
        viewModel.enterNumber(4)
        viewModel.solve()

        viewModel.reset()

        assertInitialPuzzle()
    }

    private fun assertInitialPuzzle() {
        val expected = intArrayOf(
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
        assertArrayEquals(expected, viewModel.uiState.board.toIntArray())
        assertArrayEquals(expected, viewModel.uiState.originalBoard.toIntArray())
        assertEquals(30, viewModel.uiState.board.clueCount)
        assertEquals(SudokuMode.EDIT, viewModel.uiState.mode)
        assertNull(viewModel.uiState.selectedCell)
        assertTrue(viewModel.uiState.conflicts.isEmpty())
    }
}
