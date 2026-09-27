package com.example.sudokusolver.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sudokusolver.ui.theme.SudokuSolverTheme

@Composable
fun NumberPad(
    onNumberClick: (Int) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (column in 0 until 3) {
                    val number = row * 3 + column + 1
                    Button(
                        onClick = { onNumberClick(number) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(number.toString())
                    }
                }
            }
        }
        Button(
            onClick = onClearClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NumberPadPreview() {
    SudokuSolverTheme {
        NumberPad(onNumberClick = {}, onClearClick = {})
    }
}
