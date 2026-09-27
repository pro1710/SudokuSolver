package com.example.sudokusolver

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sudokusolver.ui.theme.SudokuSolverTheme

@Composable
fun SudokuBoard(
    board: IntArray,
    selectedCell: Int?,
    onCellSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0 until 9) {
                Row(modifier = Modifier.weight(1f)) {
                    for (column in 0 until 9) {
                        val index = row * 9 + column
                        SudokuCell(
                            value = board[index],
                            isSelected = selectedCell == index,
                            onClick = {
                                Log.d(
                                    "SudokuBoard",
                                    "Cell clicked: index=$index, row=$row, column=$column, value=${board[index]}"
                                )
                                onCellSelected(index)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        )
                    }
                }
            }
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            val cellWidth = size.width / 9
            val cellHeight = size.height / 9

            for (line in 0..9) {
                val strokeWidth = if (line % 3 == 0) 3.dp.toPx() else 1.dp.toPx()
                // Keep the entire outer border inside the drawing area.
                val x = (line * cellWidth).coerceIn(strokeWidth / 2, size.width - strokeWidth / 2)
                val y = (line * cellHeight).coerceIn(strokeWidth / 2, size.height - strokeWidth / 2)

                drawLine(
                    color = lineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}

@Composable
fun SudokuCell(
    value: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (value == 0) "" else value.toString(),
            color = textColor,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SudokuBoardPreview() {
    SudokuSolverTheme {
        SudokuBoard(
            board = IntArray(81),
            selectedCell = 0,
            onCellSelected = {}
        )
    }
}
