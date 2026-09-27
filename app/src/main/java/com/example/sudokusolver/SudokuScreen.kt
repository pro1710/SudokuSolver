package com.example.sudokusolver

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sudokusolver.domain.SudokuSolver
import com.example.sudokusolver.domain.SudokuValidator
import com.example.sudokusolver.ui.theme.SudokuSolverTheme

@Composable
fun SudokuScreen(modifier: Modifier = Modifier) {
    var board by rememberSaveable { mutableStateOf(createInitialBoard()) }
    var originalBoard by rememberSaveable { mutableStateOf(board.copyOf()) }
    var selectedCell by rememberSaveable { mutableStateOf<Int?>(null) }
    var mode by rememberSaveable { mutableStateOf(SudokuMode.EDIT) }
    val conflicts = remember(board) {
        SudokuValidator.findConflicts(board)
    }

    LaunchedEffect(board) {
        Log.d("SudokuScreen", "Validation completed: conflicts=${conflicts.sorted()}")
        if (conflicts.isNotEmpty()) {
            Log.w("SudokuScreen", "Board contains ${conflicts.size} conflicting cells")
        }
    }

    LaunchedEffect(selectedCell) {
        Log.d("SudokuScreen", "Selected cell changed: index=$selectedCell")
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SudokuTitle()
        SudokuBoard(
            board = board,
            originalBoard = originalBoard,
            selectedCell = selectedCell,
            conflicts = conflicts,
            isEditable = mode == SudokuMode.EDIT,
            onCellSelected = { index -> selectedCell = index }
        )
        if (mode == SudokuMode.EDIT) {
            NumberPad(
                onNumberClick = { number ->
                    val index = selectedCell
                    if (index == null) {
                        Log.w("SudokuScreen", "Number ignored: number=$number, no cell selected")
                    } else {
                        val previousValue = board[index]
                        val newBoard = board.copyOf()
                        newBoard[index] = number
                        board = newBoard
                        val newOriginalBoard = originalBoard.copyOf()
                        newOriginalBoard[index] = number
                        originalBoard = newOriginalBoard
                        Log.d(
                            "SudokuScreen",
                            "Cell value changed: index=$index, from=$previousValue, to=$number"
                        )
                    }
                },
                onClearClick = {
                    val index = selectedCell
                    if (index == null) {
                        Log.w("SudokuScreen", "Clear ignored: no cell selected")
                    } else {
                        val previousValue = board[index]
                        val newBoard = board.copyOf()
                        newBoard[index] = 0
                        board = newBoard
                        val newOriginalBoard = originalBoard.copyOf()
                        newOriginalBoard[index] = 0
                        originalBoard = newOriginalBoard
                        Log.d("SudokuScreen", "Cell cleared: index=$index, previousValue=$previousValue")
                    }
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (mode == SudokuMode.EDIT) {
                Button(
                    onClick = {
                        if (conflicts.isNotEmpty()) {
                            Log.w("SudokuScreen", "Solve rejected: conflicts=${conflicts.sorted()}")
                        } else {
                            val workingBoard = board.copyOf()
                            val clueCount = originalBoard.count { value -> value != 0 }
                            Log.i("SudokuScreen", "Starting Sudoku solver: clues=$clueCount")
                            if (SudokuSolver.solve(workingBoard)) {
                                board = workingBoard
                                selectedCell = null
                                mode = SudokuMode.SOLUTION
                                Log.i("SudokuScreen", "Mode changed: EDIT -> SOLUTION")
                                Log.i("SudokuScreen", "Sudoku solved successfully")
                            } else {
                                Log.w("SudokuScreen", "Sudoku has no solution")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Solve")
                }
            } else {
                Button(
                    onClick = {
                        board = originalBoard.copyOf()
                        selectedCell = null
                        mode = SudokuMode.EDIT
                        Log.i("SudokuScreen", "Mode changed: SOLUTION -> EDIT; clues restored")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit clues")
                }
            }
            Button(
                onClick = {
                    board = createInitialBoard()
                    originalBoard = board.copyOf()
                    selectedCell = null
                    if (mode != SudokuMode.EDIT) {
                        Log.i("SudokuScreen", "Mode changed: SOLUTION -> EDIT; reset")
                    }
                    mode = SudokuMode.EDIT
                    Log.i("SudokuScreen", "Board reset to built-in puzzle")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
        }
    }
}

private fun createInitialBoard(): IntArray = intArrayOf(
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

@Preview(showBackground = true)
@Composable
fun SudokuScreenPreview() {
    SudokuSolverTheme {
        SudokuScreen()
    }
}
