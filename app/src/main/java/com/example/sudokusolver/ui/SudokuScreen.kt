package com.example.sudokusolver.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    BoxWithConstraints(modifier = modifier.fillMaxSize().padding(16.dp)) {
        val controlsMaxHeight = maxHeight * 0.45f
        if (maxWidth > maxHeight) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SudokuBoardPane(
                    uiState = uiState,
                    onCellSelected = onCellSelected,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                SudokuControls(
                    mode = uiState.mode,
                    onNumberClick = onNumberClick,
                    onClearClick = onClearClick,
                    onSolveClick = onSolveClick,
                    onEditCluesClick = onEditCluesClick,
                    onResetClick = onResetClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SudokuBoardPane(
                    uiState = uiState,
                    onCellSelected = onCellSelected,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                SudokuControls(
                    mode = uiState.mode,
                    onNumberClick = onNumberClick,
                    onClearClick = onClearClick,
                    onSolveClick = onSolveClick,
                    onEditCluesClick = onEditCluesClick,
                    onResetClick = onResetClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        // Leave room for the board when the number pad needs to scroll.
                        .heightIn(max = controlsMaxHeight)
                )
            }
        }
    }
}

@Composable
private fun SudokuBoardPane(
    uiState: SudokuUiState,
    onCellSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        SudokuGrid(
            board = uiState.board,
            originalBoard = uiState.originalBoard,
            selectedCell = uiState.selectedCell,
            conflicts = uiState.conflicts,
            isEditable = uiState.mode == SudokuMode.EDIT,
            onCellSelected = onCellSelected
        )
    }
}

@Composable
private fun SudokuControls(
    mode: SudokuMode,
    onNumberClick: (Int) -> Unit,
    onClearClick: () -> Unit,
    onSolveClick: () -> Unit,
    onEditCluesClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (mode == SudokuMode.EDIT) {
            NumberPad(
                onNumberClick = onNumberClick,
                onClearClick = onClearClick,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (mode == SudokuMode.EDIT) {
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

@Preview(name = "Portrait", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Landscape", showBackground = true, widthDp = 800, heightDp = 360)
@Preview(
    name = "Small landscape, large text",
    showBackground = true,
    widthDp = 640,
    heightDp = 320,
    fontScale = 1.5f
)
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
