# Step 05C — Conflict highlighting in Compose

## Goal

Show conflicting Sudoku cells using Material error colors immediately after
number entry or Clear. Keep all cells editable so the user can correct them.
Log validation results from the screen, while the validator stays pure Kotlin.

## Starting point

Start from Step 05B, commit `b8e553c`
(`step-05b: validate rows columns and boxes`). `SudokuValidator.findConflicts`
already returns conflicting indices for rows, columns, and boxes. Its eight
JVM tests pass, but the UI does not call it yet.

`SudokuScreen` owns `board` and `selectedCell`. Editing makes a copy of the
array and assigns it to state. `SudokuBoard` receives those values and forwards
cell clicks to the screen.

## Concepts introduced

- **A derived value** is calculated from existing state. Conflicts depend on
  the board, so calculate them from it instead of maintaining a second
  independently editable state variable.
- **`remember(board)`** caches a calculation while its key is unchanged.
  Unlike the unkeyed `remember` used to initialize state, this calculation
  runs again when editing replaces the board array.
- **Set membership**, written `index in conflicts`, tells each cell whether
  its index appears in the validator's result.
- **`when` with conditions** selects the first matching branch. Put the
  conflict branch first so it takes precedence over selection.
- **Material color roles** pair a background with readable foreground text.
  `errorContainer` and `onErrorContainer` communicate the error state using
  the current theme.

Android's [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state)
guide explains remembered calculations and keys. The
[Material 3 guide](https://developer.android.com/develop/ui/compose/designsystems/material3)
describes theme color schemes and their semantic roles.

## Implementation

### 1. Calculate conflicts from screen state

In `SudokuScreen.kt`, import the existing validator:

```kotlin
import com.example.sudokusolver.domain.SudokuValidator
```

After the `selectedCell` declaration, add:

```kotlin
val conflicts = remember(board) {
    SudokuValidator.findConflicts(board)
}
```

The initial puzzle is validated on first composition. Each existing number
or Clear handler assigns a new array, so Compose recalculates the conflicts
for that array. Selecting a different cell leaves the board key unchanged
and reuses the result.

Keep the copy-before-write handlers from Step 04. Mutating `board[index]`
directly would neither reliably notify Compose nor change this cache's key.
`conflicts` needs no `mutableStateOf`: it is always computed from the current
board, keeping the board as the source of truth.

### 2. Log completed validation from the screen

Immediately after that calculation, add:

```kotlin
LaunchedEffect(board) {
    Log.d("SudokuScreen", "Validation completed: conflicts=${conflicts.sorted()}")
    if (conflicts.isNotEmpty()) {
        Log.w("SudokuScreen", "Board contains ${conflicts.size} conflicting cells")
    }
}
```

Leave the existing `LaunchedEffect(selectedCell)` in place. The new effect
runs initially and when the board key changes. It records the computed
result without placing logging in the composable body or the validator.
`sorted()` gives the log a predictable index order; the value passed to the
UI remains a set.

The board is the effect's key because even edits that produce the same conflict
set represent a newly validated board. Selection alone does not restart this
effect. A warning reports a recoverable puzzle problem, not an app failure.

As in Step 03A, effects observe composed state; very rapid state changes can
be combined before an effect runs. The existing click/edit callbacks record
the individual user events. See Android's
[Side-effects in Compose](https://developer.android.com/develop/ui/compose/side-effects)
guide for effect keys and restarts.

### 3. Pass the set into the board

In `SudokuBoard.kt`, add a required parameter after `selectedCell`:

```kotlin
fun SudokuBoard(
    board: IntArray,
    selectedCell: Int?,
    conflicts: Set<Int>,
    onCellSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
)
```

Keep `@Composable` and the existing body. In the screen's board call, pass
the calculated value:

```kotlin
SudokuBoard(
    board = board,
    selectedCell = selectedCell,
    conflicts = conflicts,
    onCellSelected = { index -> selectedCell = index }
)
```

The board renders the result supplied by its parent; it does not need its own
validator call or stored copy of the result.

### 4. Give each cell its conflict flag

Add `isConflicting: Boolean` after `isSelected` in `SudokuCell`'s parameters.
Then add this argument to the cell call in the board's inner loop:

```kotlin
isConflicting = index in conflicts,
```

For example, if the validator returns `setOf(2, 4)`, those two cells receive
`true`; every other cell receives `false`.

Replace the two existing color expressions in `SudokuCell` with:

```kotlin
val backgroundColor = when {
    isConflicting -> MaterialTheme.colorScheme.errorContainer
    isSelected -> MaterialTheme.colorScheme.primaryContainer
    else -> MaterialTheme.colorScheme.surface
}
val textColor = when {
    isConflicting -> MaterialTheme.colorScheme.onErrorContainer
    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
    else -> MaterialTheme.colorScheme.onSurface
}
```

Conflict has priority in both expressions. A selected conflicting cell keeps
its error colors; it remains selected internally, so number entry and Clear
still act on it. Once its conflict is removed, it returns to the usual
selection colors. Keep the existing `background(backgroundColor)`,
`color = textColor`, click handling, and Canvas grid drawing.

### 5. Update the board preview

The preview needs the new `conflicts` argument. Use a tiny explicit example
to inspect the priority rule. At the beginning of `SudokuBoardPreview`, before
the theme block, add:

```kotlin
val board = IntArray(81)
board[0] = 5
board[8] = 5
```

Inside the existing `SudokuSolverTheme` block, use:

```kotlin
SudokuBoard(
    board = board,
    selectedCell = 0,
    conflicts = setOf(0, 8),
    onCellSelected = {}
)
```

Both `5`s should use error colors, including the selected first cell. This
preview supplies fixed visual state; the full `SudokuScreenPreview` and running
app calculate their conflicts through the validator.

## Logging / troubleshooting

Filter Logcat with:

```text
package:com.example.sudokusolver tag:SudokuScreen level:DEBUG
```

On the unchanged built-in puzzle, expect:

```text
D/SudokuScreen: Validation completed: conflicts=[]
```

Select index 2 and enter `7`. The original `7` is at index 4, so expect:

```text
D/SudokuScreen: Cell value changed: index=2, from=0, to=7
D/SudokuScreen: Validation completed: conflicts=[2, 4]
W/SudokuScreen: Board contains 2 conflicting cells
```

Clear index 2:

```text
D/SudokuScreen: Cell cleared: index=2, previousValue=7
D/SudokuScreen: Validation completed: conflicts=[]
```

Selecting cells without editing produces selection/click logs but no new
validation message. An empty conflict set means no current duplicate-rule
violations; it does not prove the puzzle is complete or solvable.

- **Highlights remain after Clear:** preserve the copied-array assignment and
  use `remember(board)`, not an unkeyed `remember` for conflicts.
- **Only the edited cell highlights:** pass the complete validator result;
  both the new value and matching clues are conflicting cells.
- **Selection hides errors:** place `isConflicting` before `isSelected` in
  both color expressions.
- **Text is hard to read:** use `onErrorContainer` with `errorContainer`.
- **Logs repeat when only selection changes:** use `LaunchedEffect(board)`
  rather than logging directly during composition.
- **Preview has a missing argument:** update every board call with `conflicts`
  and every cell call with `isConflicting`.
- **Emulator is black without a crash:** use the cold-boot troubleshooting
  instructions in [Step 04](04-editable-board.md); do not assume it is a
  validation error.

## Run / test

In Android Studio, build and run on the course's Android 15 / API 35 device in
portrait. The existing validator tests remain unchanged and can be run as
`SudokuValidatorTest`; the new behavior needs these interactive checks.

First confirm the original puzzle has no error highlights. Test each case
below against that original puzzle, clearing the edited cell after each case
to restore it. Visual row/column numbers in the table count from one; indices
in the code and logs count from zero.

| Rule | Cell to edit | Enter | Expected conflicting indices |
|---|---|---|---|
| Row only | Row 1, column 3 (index 2) | `7` | `[2, 4]` |
| Column only | Row 1, column 4 (index 3) | `8` | `[3, 39]` |
| Box only | Row 2, column 2 (index 10) | `8` | `[10, 20]` |

For each case, confirm both cells use error colors, the log reports exactly
two conflicting cells, and Clear removes both error highlights. The cleared
cell should retain its normal selection background.

Also verify:

1. Tap between conflicting and non-conflicting cells without editing. Error
   colors stay on conflicts even when selected, and validation does not log
   again merely because selection changes.
2. In the row example, replace the entered `7` with `4` instead of clearing.
   The conflict should disappear. Clear the `4` afterwards to restore the
   original puzzle.
3. Check the preview's selected conflicting cell and the running app in both
   light and dark appearance. Text and grid lines should remain readable.
4. On a fresh launch, pressing a number or Clear without selecting a cell
   should keep the board unchanged and produce the existing warning.

## Expected result

- ✓ Initial puzzle validation reports no conflicts.
- ✓ Row, column, and box conflicts highlight every participating cell.
- ✓ Error colors take priority over selected-cell colors.
- ✓ Correcting or clearing a value updates highlights immediately.
- ✓ Validation logging stays in the screen and follows board changes.

## Architecture after this step

```text
SudokuScreen
    ├── board state → SudokuValidator.findConflicts(board)
    │                         ↓
    │                   conflicts: Set<Int>
    ├── validation logs in LaunchedEffect(board)
    ├── SudokuBoard(board, selectedCell, conflicts)
    │       └── SudokuCell(isSelected, isConflicting)
    └── NumberPad → copied-array update → board state
```

The domain layer decides which cells conflict. The screen connects that result
to Compose, and each cell chooses its visual appearance from the supplied flags.
