# Step 06B — Connect the solver to Compose

## Goal

Add a **Solve** button that fills the displayed puzzle using the tested
backtracking solver. Reject solving when conflicts exist, and distinguish
accepted clues from solver-generated digits using font weight and color.

## Starting point

Start from Step 06A, commit `908fba1`
(`step-06a: implement sudoku backtracking solver`). The screen already owns
an editable board, selection, and derived conflicts. The domain solver exposes
`solve(IntArray): Boolean` and mutates its argument on success. Its five tests
and the validator's eight tests cover the domain behavior.

Change only the screen and board UI, their previews, and this documentation.
Keep the domain implementation and project dependencies unchanged.

## Concepts introduced

**Two arrays with different responsibilities:**

| Value | Meaning | When it changes |
|---|---|---|
| `board` | Values currently displayed, including any solver-filled digits | Manual edits and successful solves |
| `originalBoard` | Accepted clues, with zeroes where the solver supplies values | Manual edits only |

Here, “original” means the user's clue board, not a permanently frozen copy of
the built-in puzzle. A manually entered digit becomes a clue even if it creates
a conflict; the existing validator will report that conflict.

**A working copy** isolates the solver's temporary mutations from published
Compose state. Assign the completed array to `board` only after success.
This continues the copy-before-write approach from Step 04. Android's
[State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state)
guide explains observable state and the problems caused by mutating an
unobserved object in place.

**Text styling** communicates where a digit came from. Clues use bold text;
solver-filled digits use normal weight and the theme's primary color.
Conflict and selection foreground colors still take priority for readability.

## Implementation

### 1. Remember a separate clue board

In `SudokuScreen.kt`, immediately after the existing `board` declaration, add:

```kotlin
var originalBoard by remember {
    mutableStateOf(board.copyOf())
}
```

Use a separate array so the clue values do not share mutable storage with the
displayed board. This initializer runs when the screen first enters the
composition. Do not use `remember(board)` here: that would recreate the clue
board whenever solving replaces the displayed array, losing the distinction.

Keep `selectedCell`, conflict calculation, and the existing effects in place.

### 2. Update both arrays after manual input

In the number handler, immediately after the existing `board = newBoard`, add:

```kotlin
val newOriginalBoard = originalBoard.copyOf()
newOriginalBoard[index] = number
originalBoard = newOriginalBoard
```

In the Clear handler, after its `board = newBoard`, add:

```kotlin
val newOriginalBoard = originalBoard.copyOf()
newOriginalBoard[index] = 0
originalBoard = newOriginalBoard
```

Both additions belong inside the existing non-null selection branches.
The warning behavior when nothing is selected remains unchanged.

Copy the clue array itself and update only the selected entry. Assigning
`originalBoard = newBoard.copyOf()` after an edit would accidentally promote
all previously solver-filled digits to clues. Each array must keep its own
unchanged entries.

### 3. Pass clue information to the cells

In `SudokuBoard.kt`, add `originalBoard: IntArray` immediately after the
`board` parameter. Pass it from the screen:

```kotlin
SudokuBoard(
    board = board,
    originalBoard = originalBoard,
    selectedCell = selectedCell,
    conflicts = conflicts,
    onCellSelected = { index -> selectedCell = index }
)
```

Add `isOriginal: Boolean` after `value` in `SudokuCell`'s parameters. In the
board's existing cell call, pass:

```kotlin
isOriginal = originalBoard[index] != 0,
```

The displayed digit still comes from `board[index]`. The clue board determines
its origin, not which value to display.

### 4. Style clues and generated digits

Import `androidx.compose.ui.text.font.FontWeight` in `SudokuBoard.kt`.
Keep the background-color expression unchanged. Replace the text-color
expression with:

```kotlin
val textColor = when {
    isConflicting -> MaterialTheme.colorScheme.onErrorContainer
    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
    isOriginal -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.primary
}
```

In the cell's `Text` call, add:

```kotlin
fontWeight = if (isOriginal) FontWeight.Bold else FontWeight.Normal,
```

A clue is bold regardless of selection or conflict. A generated digit is
normal weight. Its primary color applies when it is neither selected nor
conflicting; the earlier branches supply readable text colors for those
backgrounds. Thus font weight continues to distinguish origin even when
selection temporarily overrides text color.

See Android's [Style text](https://developer.android.com/develop/ui/compose/text/style-text)
guide for `color` and `fontWeight`, and the
[Material 3 guide](https://developer.android.com/develop/ui/compose/designsystems/material3)
for theme color roles.

### 5. Add Solve after the number pad

In `SudokuScreen.kt`, add imports for `SudokuSolver`, `Button`, `Text`, and
`fillMaxWidth`:

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.example.sudokusolver.domain.SudokuSolver
```

Inside the screen's Column, after the `NumberPad(...)` call, add:

```kotlin
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
                Log.i("SudokuScreen", "Sudoku solved successfully")
            } else {
                Log.w("SudokuScreen", "Sudoku has no solution")
            }
        }
    },
    modifier = Modifier.fillMaxWidth()
) {
    Text("Solve")
}
```

The guard prevents the screen from invoking the solver on a conflicting board.
The solver retains its own input check for callers outside Compose. Keep the
button enabled so pressing it can report a rejection through Logcat.

`count { value -> value != 0 }` counts accepted clues. The built-in puzzle has
30. The temporary working array contains the current displayed values, and the
solver is allowed to mutate it while searching. On success, assigning it to
`board` triggers rendering and the existing validation effect. On failure,
neither published array is reassigned. `originalBoard` never changes in the
Solve handler.

The official [Button guide](https://developer.android.com/develop/ui/compose/components/button)
explains `onClick` and button content. The solver call runs synchronously in
this callback at this stage. The teaching puzzle is small, but difficult
searches can delay UI response; this step adds no background execution.

### 6. Update the board preview

In `SudokuBoardPreview`, after the two existing `5` assignments, add:

```kotlin
val originalBoard = board.copyOf()
board[4] = 7
```

Pass `originalBoard = originalBoard` to its `SudokuBoard` call. Retain
`selectedCell = 0`, `conflicts = setOf(0, 8)`, and the empty selection callback.

The two `5`s are clues and should be bold with error colors. The `7` was added
only to the displayed array, so it represents a generated digit and should
use normal weight and primary color. This is a visual fixture; the preview
does not invoke the solver. `SudokuScreenPreview` still starts with the actual
puzzle and can exercise Solve in interactive mode.

## Logging / troubleshooting

Use `package:com.example.sudokusolver tag:SudokuScreen level:DEBUG` in Logcat
to include Debug, Info, and Warning messages. Solving the initial puzzle:

```text
I/SudokuScreen: Starting Sudoku solver: clues=30
I/SudokuScreen: Sudoku solved successfully
D/SudokuScreen: Validation completed: conflicts=[]
```

Before solving, enter `7` at index 2 in the initial puzzle. It conflicts with
the existing `7` at index 4, and Solve reports:

```text
W/SudokuScreen: Solve rejected: conflicts=[2, 4]
```

Clear that edit, then enter `1` at index 2. This is the conflict-free but
unsolvable fixture tested in Step 06A:

```text
I/SudokuScreen: Starting Sudoku solver: clues=31
W/SudokuScreen: Sudoku has no solution
```

Failure feedback in this step is the warning log; the displayed puzzle stays
as it was. There is no new dialog or status message.

- **Every solved digit becomes bold:** do not assign the working solution to
  `originalBoard`, and do not key its initializer by `board`.
- **Editing one generated digit makes all of them clues:** update a copy of
  `originalBoard`, not a copy of the displayed solution.
- **The board does not refresh after solving:** pass a working copy to the
  solver and assign that array to Compose state after success.
- **Conflict colors disappear:** keep the conflict branch first in both
  color expressions.
- **Missing arguments in previews:** update both board calls with
  `originalBoard` and the cell call with `isOriginal`.
- **Editing after solving leaves other generated digits visible:** expected
  for this step. The number pad remains active, and another Solve uses the
  current displayed board. Step 06C will introduce explicit Edit/Solution
  modes and restoring accepted clues before editing.
- **Rotation resets the puzzle:** these values still use `remember`. They do
  not survive normal Activity recreation yet.
- **Controls are cramped:** the existing layout remains a vertical Column
  with a width-based square board. Review this step in portrait; adaptive
  layout remains deferred.

## Run / test

Build and run in Android Studio on the course's Android 15 / API 35 device.
Use these checks in order so each begins with the appropriate board:

1. On a fresh launch, confirm the original 30 clues are bold and Solve is
   visible below the number pad.
2. Select row 1, column 3 (index 2), enter `7`, and press Solve. Confirm the
   conflict remains highlighted, the board is not filled, and the rejection
   log lists `[2, 4]`. Clear the edit.
3. In that same cell, enter `1` and press Solve. Confirm there are no duplicate
   highlights, the no-solution warning appears, and no attempted solver values
   appear on the board. Clear the edit to restore the initial puzzle.
4. Press Solve. Confirm all cells fill, no conflicts remain, and the first
   row reads `5 3 4 6 7 8 9 1 2`. Compare the full board with the expected
   solution in the Step 06A test if needed.
5. Tap an original clue to move selection away from index 2. Confirm original
   clues are bold and generated digits such as the `4` at index 2 are normal
   weight and primary-colored when unselected. Inspect light and dark themes.
   A theme change that recreates the Activity requires solving again.
6. Select the generated `4` at index 2 and press `4` on the pad. It should
   become bold because it is now a manually accepted clue. Other generated
   digits should remain normal weight. Press Solve again: its log should now
   count 31 accepted clues, even though the displayed board is already full.
7. Clear that `4`, then Solve once more. It should be filled as a generated
   digit again, and the clue count should return to 30.
8. Confirm conflict highlighting, selection, and number/Clear warnings without
   a selection continue to behave as before. Use a fresh launch for the
   no-selection check.

The existing `SudokuSolverTest` and `SudokuValidatorTest` remain the domain
checks: 13 tests total. No new test dependencies or UI test framework are
introduced. The instructor validates the Compose integration manually.

## Expected result

- ✓ Solve rejects boards with conflicts before starting a search.
- ✓ A successful solve publishes a completed copy of the displayed board.
- ✓ Accepted clues remain bold; generated digits have normal weight and accent color.
- ✓ Manual edits update both boards, while solving updates only the display.
- ✓ Rejection, success, and no-solution outcomes have meaningful logs.

## Architecture after this step

```text
SudokuScreen
    ├── board: displayed values
    ├── originalBoard: accepted clues
    ├── SudokuValidator → conflicts
    ├── NumberPad → update both boards at selected index
    ├── Solve → reject conflicts or solve a working copy
    │                         └── success → replace board only
    └── SudokuBoard(board, originalBoard, ...)
            └── SudokuCell(value, isOriginal, ...)
```

The screen coordinates the mutable solver API with Compose's state updates.
Clue tracking remains separate from the solved display so the next step can
introduce explicit editing and solution modes incrementally.
