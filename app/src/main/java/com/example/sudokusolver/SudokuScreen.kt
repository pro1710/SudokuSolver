package com.example.sudokusolver

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sudokusolver.ui.theme.SudokuSolverTheme

@Composable
fun SudokuScreen(modifier: Modifier = Modifier) {
    var board by remember {
        mutableStateOf(
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
    }
    var selectedCell by remember {
        mutableStateOf<Int?>(null)
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
            selectedCell = selectedCell,
            onCellSelected = { index -> selectedCell = index }
        )
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
                    Log.d("SudokuScreen", "Cell cleared: index=$index, previousValue=$previousValue")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SudokuScreenPreview() {
    SudokuSolverTheme {
        SudokuScreen()
    }
}
