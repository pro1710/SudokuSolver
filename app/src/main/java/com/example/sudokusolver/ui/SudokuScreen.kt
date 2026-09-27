package com.example.sudokusolver.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sudokusolver.ui.theme.SudokuSolverTheme

@Composable
fun SudokuScreen(
    modifier: Modifier = Modifier,
    viewModel: SudokuViewModel = viewModel()
) {
    SudokuScreenContent(
        uiState = viewModel.uiState,
        onCellSelected = viewModel::selectCell,
        onNumberClick = viewModel::enterNumber,
        onClearClick = viewModel::clearCell,
        onSolveClick = viewModel::solve,
        onEditCluesClick = viewModel::editClues,
        onResetClick = viewModel::reset,
        modifier = modifier
    )
}

@Composable
fun SudokuScreenContent(
    uiState: SudokuUiState,
    onCellSelected: (Int) -> Unit,
    onNumberClick: (Int) -> Unit,
    onClearClick: () -> Unit,
    onSolveClick: () -> Unit,
    onEditCluesClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SudokuTitle()
        SudokuGrid(
            board = uiState.board,
            originalBoard = uiState.originalBoard,
            selectedCell = uiState.selectedCell,
            conflicts = uiState.conflicts,
            isEditable = uiState.mode == SudokuMode.EDIT,
            onCellSelected = onCellSelected
        )
        if (uiState.mode == SudokuMode.EDIT) {
            NumberPad(
                onNumberClick = onNumberClick,
                onClearClick = onClearClick
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.mode == SudokuMode.EDIT) {
                Button(
                    onClick = onSolveClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Solve")
                }
            } else {
                Button(
                    onClick = onEditCluesClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit clues")
                }
            }
            Button(
                onClick = onResetClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
        }
    }
}

@Composable
fun SudokuTitle(modifier: Modifier = Modifier) {
    Text(
        text = "Sudoku Solver",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun SudokuTitlePreview() {
    SudokuSolverTheme {
        SudokuTitle()
    }
}

@Preview(showBackground = true)
@Composable
fun SudokuScreenPreview() {
    SudokuSolverTheme {
        SudokuScreenContent(
            uiState = SudokuUiState(),
            onCellSelected = {},
            onNumberClick = {},
            onClearClick = {},
            onSolveClick = {},
            onEditCluesClick = {},
            onResetClick = {}
        )
    }
}
