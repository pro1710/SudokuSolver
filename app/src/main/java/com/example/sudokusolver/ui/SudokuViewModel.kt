package com.example.sudokusolver.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.sudokusolver.domain.SudokuBoard
import com.example.sudokusolver.domain.SudokuSolver
import com.example.sudokusolver.domain.SudokuValidator
import com.example.sudokusolver.util.AndroidAppLogger
import com.example.sudokusolver.util.AppLogger

class SudokuViewModel(
    private val logger: AppLogger = AndroidAppLogger
) : ViewModel() {
    var uiState by mutableStateOf(SudokuUiState())
        private set

    init {
        logger.debug("SudokuViewModel", "ViewModel created")
    }

    fun selectCell(index: Int) {
        if (uiState.mode != SudokuMode.EDIT) return
        uiState = uiState.copy(selectedCell = index)
        logger.debug("SudokuViewModel", "Selected cell changed: index=$index")
    }

    fun enterNumber(number: Int) {
        updateSelectedCell(number)
    }

    fun clearCell() {
        updateSelectedCell(0)
    }

    private fun updateSelectedCell(number: Int) {
        if (uiState.mode != SudokuMode.EDIT) return
        val index = uiState.selectedCell
        if (index == null) {
            if (number == 0) {
                logger.warning("SudokuViewModel", "Clear ignored: no cell selected")
            } else {
                logger.warning("SudokuViewModel", "Number ignored: number=$number, no cell selected")
            }
            return
        }

        val previousValue = uiState.board[index]
        val newBoard = uiState.board.withValue(index, number)
        val newOriginalBoard = uiState.originalBoard.withValue(index, number)
        uiState = uiState.copy(
            board = newBoard,
            originalBoard = newOriginalBoard,
            conflicts = findConflicts(newBoard)
        )
        if (number == 0) {
            logger.debug("SudokuViewModel", "Cell cleared: index=$index, previousValue=$previousValue")
        } else {
            logger.debug(
                "SudokuViewModel",
                "Cell value changed: index=$index, from=$previousValue, to=$number"
            )
        }
    }

    fun solve() {
        if (uiState.mode != SudokuMode.EDIT) return
        if (uiState.conflicts.isNotEmpty()) {
            logger.warning("SudokuViewModel", "Solve rejected: conflicts=${uiState.conflicts.sorted()}")
            return
        }

        val clueCount = uiState.originalBoard.clueCount
        logger.info("SudokuViewModel", "Starting Sudoku solver: clues=$clueCount")
        val solvedBoard = SudokuSolver.solve(uiState.board)
        if (solvedBoard != null) {
            uiState = uiState.copy(
                board = solvedBoard,
                selectedCell = null,
                conflicts = findConflicts(solvedBoard),
                mode = SudokuMode.SOLUTION
            )
            logger.info("SudokuViewModel", "Mode changed: EDIT -> SOLUTION")
            logger.info("SudokuViewModel", "Sudoku solved successfully")
        } else {
            logger.warning("SudokuViewModel", "Sudoku has no solution")
        }
    }

    fun editClues() {
        if (uiState.mode != SudokuMode.SOLUTION) return
        val clueBoard = uiState.originalBoard
        uiState = uiState.copy(
            board = clueBoard,
            selectedCell = null,
            conflicts = findConflicts(clueBoard),
            mode = SudokuMode.EDIT
        )
        logger.info("SudokuViewModel", "Mode changed: SOLUTION -> EDIT; clues restored")
    }

    fun reset() {
        val previousMode = uiState.mode
        uiState = SudokuUiState()
        if (previousMode != SudokuMode.EDIT) {
            logger.info("SudokuViewModel", "Mode changed: SOLUTION -> EDIT; reset")
        }
        logger.info("SudokuViewModel", "Board reset to built-in puzzle")
    }

    private fun findConflicts(board: SudokuBoard): Set<Int> {
        val conflicts = SudokuValidator.findConflicts(board)
        logger.debug("SudokuViewModel", "Validation completed: conflicts=${conflicts.sorted()}")
        if (conflicts.isNotEmpty()) {
            logger.warning("SudokuViewModel", "Board contains ${conflicts.size} conflicting cells")
        }
        return conflicts
    }
}
