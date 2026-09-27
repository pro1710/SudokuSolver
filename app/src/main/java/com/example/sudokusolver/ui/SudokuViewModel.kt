package com.example.sudokusolver.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.sudokusolver.domain.SudokuSolver
import com.example.sudokusolver.domain.SudokuValidator

class SudokuViewModel : ViewModel() {
    var uiState by mutableStateOf(SudokuUiState())
        private set

    init {
        Log.d("SudokuViewModel", "ViewModel created")
    }

    fun selectCell(index: Int) {
        if (uiState.mode != SudokuMode.EDIT) return
        uiState = uiState.copy(selectedCell = index)
        Log.d("SudokuViewModel", "Selected cell changed: index=$index")
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
                Log.w("SudokuViewModel", "Clear ignored: no cell selected")
            } else {
                Log.w("SudokuViewModel", "Number ignored: number=$number, no cell selected")
            }
            return
        }

        val previousValue = uiState.board[index]
        val newBoard = uiState.board.copyOf()
        newBoard[index] = number
        val newOriginalBoard = uiState.originalBoard.copyOf()
        newOriginalBoard[index] = number
        uiState = uiState.copy(
            board = newBoard,
            originalBoard = newOriginalBoard,
            conflicts = findConflicts(newBoard)
        )
        if (number == 0) {
            Log.d("SudokuViewModel", "Cell cleared: index=$index, previousValue=$previousValue")
        } else {
            Log.d(
                "SudokuViewModel",
                "Cell value changed: index=$index, from=$previousValue, to=$number"
            )
        }
    }

    fun solve() {
        if (uiState.mode != SudokuMode.EDIT) return
        if (uiState.conflicts.isNotEmpty()) {
            Log.w("SudokuViewModel", "Solve rejected: conflicts=${uiState.conflicts.sorted()}")
            return
        }

        val workingBoard = uiState.board.copyOf()
        val clueCount = uiState.originalBoard.count { value -> value != 0 }
        Log.i("SudokuViewModel", "Starting Sudoku solver: clues=$clueCount")
        if (SudokuSolver.solve(workingBoard)) {
            uiState = uiState.copy(
                board = workingBoard,
                selectedCell = null,
                conflicts = findConflicts(workingBoard),
                mode = SudokuMode.SOLUTION
            )
            Log.i("SudokuViewModel", "Mode changed: EDIT -> SOLUTION")
            Log.i("SudokuViewModel", "Sudoku solved successfully")
        } else {
            Log.w("SudokuViewModel", "Sudoku has no solution")
        }
    }

    fun editClues() {
        if (uiState.mode != SudokuMode.SOLUTION) return
        val clueBoard = uiState.originalBoard.copyOf()
        uiState = uiState.copy(
            board = clueBoard,
            selectedCell = null,
            conflicts = findConflicts(clueBoard),
            mode = SudokuMode.EDIT
        )
        Log.i("SudokuViewModel", "Mode changed: SOLUTION -> EDIT; clues restored")
    }

    fun reset() {
        val previousMode = uiState.mode
        uiState = SudokuUiState()
        if (previousMode != SudokuMode.EDIT) {
            Log.i("SudokuViewModel", "Mode changed: SOLUTION -> EDIT; reset")
        }
        Log.i("SudokuViewModel", "Board reset to built-in puzzle")
    }

    private fun findConflicts(board: IntArray): Set<Int> {
        val conflicts = SudokuValidator.findConflicts(board)
        Log.d("SudokuViewModel", "Validation completed: conflicts=${conflicts.sorted()}")
        if (conflicts.isNotEmpty()) {
            Log.w("SudokuViewModel", "Board contains ${conflicts.size} conflicting cells")
        }
        return conflicts
    }
}
