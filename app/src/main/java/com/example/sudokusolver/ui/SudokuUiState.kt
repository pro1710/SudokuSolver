package com.example.sudokusolver.ui

import com.example.sudokusolver.domain.SudokuBoard

data class SudokuUiState(
    val board: SudokuBoard = createInitialBoard(),
    val originalBoard: SudokuBoard = board,
    val selectedCell: Int? = null,
    val conflicts: Set<Int> = emptySet(),
    val mode: SudokuMode = SudokuMode.EDIT
)

private fun createInitialBoard(): SudokuBoard = SudokuBoard(
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
