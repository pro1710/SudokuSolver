# Step 06C — Edit and Solution modes

## Goal

Separate entering clues from viewing a solution. After solving, cells cannot be
edited and the number pad disappears. **Edit clues** returns to the accepted
clues; **Reset** restores the built-in puzzle. Preserve the current state when
the device rotates.

## Starting point

Start from Step 06B, commit `71411d5` (`step-06b: integrate solver with compose ui`).
The screen can solve a copied array and distinguish accepted clues in
`originalBoard` from generated digits in `board`. However, the number pad remains
available after solving, so editing can leave generated digits in the puzzle.

## Concepts introduced

- An **enum** defines a fixed set of named values. `SudokuMode` permits exactly
  `EDIT` and `SOLUTION`, avoiding ambiguous combinations of Boolean flags.
- A **state transition** changes which actions the screen permits. Successful
  solving moves to `SOLUTION`; returning to clues moves to `EDIT`.
- **Conditional composition** uses Kotlin `if` to include the appropriate UI.
  Changing observable state causes Compose to update that UI.
- **Saved UI state** survives Activity recreation. `remember` alone retains
  values across recomposition, but a rotation normally recreates the Activity.
  `rememberSaveable` can restore compatible values after that recreation.

Read Android's [Compose state guide](https://developer.android.com/develop/ui/compose/state)
and [saving UI state guide](https://developer.android.com/develop/ui/compose/state-saving)
for the distinction between recomposition and recreation.

```text
EDIT -- successful Solve --> SOLUTION
EDIT <-- Edit clues -------- SOLUTION

Either mode -- Reset --> EDIT with the built-in puzzle
```

An invalid or unsolvable puzzle stays in `EDIT`.

## Implementation

### 1. Name the two modes

Create `SudokuMode.kt` alongside `SudokuScreen.kt`, in
`com.example.sudokusolver`:

```kotlin
enum class SudokuMode {
    EDIT,
    SOLUTION
}
```

### 2. Make the initial puzzle reusable and save screen state

Move the existing nine rows of `intArrayOf(...)` into a private function at the
bottom of `SudokuScreen.kt`:

```kotlin
private fun createInitialBoard(): IntArray = intArrayOf(
    // Move all 81 existing values here, unchanged.
)
```

The comment above marks where to move the existing values; it is not an empty
replacement puzzle. Calling this function creates a fresh array each time,
so Reset cannot accidentally reuse an edited array.

Import `androidx.compose.runtime.saveable.rememberSaveable` and replace the three
state declarations at the start of `SudokuScreen`. Add the mode state:

```kotlin
var board by rememberSaveable { mutableStateOf(createInitialBoard()) }
var originalBoard by rememberSaveable { mutableStateOf(board.copyOf()) }
var selectedCell by rememberSaveable { mutableStateOf<Int?>(null) }
var mode by rememberSaveable { mutableStateOf(SudokuMode.EDIT) }
```

`IntArray`, nullable integers, and this serializable enum can be saved using
Android's saved-state mechanism. These are small pieces of UI state; this is
not persistent puzzle storage. On restoration, saved values replace the initial
values. Keep using copied-array updates so Compose observes edits.

Keep `remember(board) { SudokuValidator.findConflicts(board) }`. Conflicts are
derived from the restored board, so they do not need a separate saved copy.

### 3. Disable cell interaction in Solution mode

Add `isEditable: Boolean` to both `SudokuBoard` and `SudokuCell`, before their
callback parameters. Pass the value from board to cell:

```kotlin
isEditable = isEditable,
```

In `SudokuCell`, change the existing click modifier:

```kotlin
.clickable(enabled = isEditable, onClick = onClick)
```

This disables the click action itself, including its callback and click log.
See Android's [tap and press guide](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/tap-and-press)
for the behavior provided by `clickable`.

Pass this argument from the screen's board call:

```kotlin
isEditable = mode == SudokuMode.EDIT,
```

Update `SudokuBoardPreview` with `isEditable = true`. Previews must supply new
required parameters too.

### 4. Show controls appropriate to the mode

Wrap the existing `NumberPad` call in `if (mode == SudokuMode.EDIT)`. Keep its
number and Clear callbacks unchanged: manual changes still update both arrays.

Below it, replace the single Solve button with a `Row`:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    if (mode == SudokuMode.EDIT) {
        // Existing Solve button, with the success changes below.
    } else {
        // New Edit clues button.
    }
    // New Reset button, available in either mode.
}
```

Import `androidx.compose.foundation.layout.Row`. Give each button
`Modifier.weight(1f)` so the two visible actions share the row. The comments
above indicate where to place the actual buttons described next.

Keep Solve's conflict check, working copy, and failure logs. Only extend its
successful branch:

```kotlin
board = workingBoard
selectedCell = null
mode = SudokuMode.SOLUTION
Log.i("SudokuScreen", "Mode changed: EDIT -> SOLUTION")
Log.i("SudokuScreen", "Sudoku solved successfully")
```

Clearing selection removes the old editing highlight. Do not assign the solution
to `originalBoard`: that array must remain the accepted clues. Failed solving
does not change either the board or mode.

### 5. Restore clues or reset the puzzle

In the `else` branch, add a Button with `Text("Edit clues")`,
`Modifier.weight(1f)`, and this `onClick`:

```kotlin
board = originalBoard.copyOf()
selectedCell = null
mode = SudokuMode.EDIT
Log.i("SudokuScreen", "Mode changed: SOLUTION -> EDIT; clues restored")
```

This removes solver-generated digits while retaining the instructor's or
student's manually entered clues. The number pad returns automatically because
the mode changed.

After the conditional, add a Button with `Text("Reset")`,
`Modifier.weight(1f)`, and this `onClick`:

```kotlin
board = createInitialBoard()
originalBoard = board.copyOf()
selectedCell = null
if (mode != SudokuMode.EDIT) {
    Log.i("SudokuScreen", "Mode changed: SOLUTION -> EDIT; reset")
}
mode = SudokuMode.EDIT
Log.i("SudokuScreen", "Board reset to built-in puzzle")
```

Reset intentionally discards manual edits as well as the solution. The factory
supplies the original 30 clues, and validation recomputes from that board.

## Logging / troubleshooting

Filter Logcat with `package:com.example.sudokusolver tag:SudokuScreen`. Solving
the initial puzzle, then choosing Edit clues, produces these workflow messages
(existing validation and selection logs can appear between them):

```text
I/SudokuScreen: Starting Sudoku solver: clues=30
I/SudokuScreen: Mode changed: EDIT -> SOLUTION
I/SudokuScreen: Sudoku solved successfully
I/SudokuScreen: Mode changed: SOLUTION -> EDIT; clues restored
I/SudokuScreen: Board reset to built-in puzzle
```

Mode-transition logs belong in action callbacks, not the composable body.
Recomposition and restoring an existing mode after rotation are not user mode
transitions. Existing `LaunchedEffect` logs can run again after recreation.

- If generated digits remain after Edit clues, restore from `originalBoard`,
  not a copy of the solved `board`.
- If cells still select in Solution mode, check that `isEditable` reaches the
  cell's `clickable(enabled = ...)` call.
- If Reset preserves edited clues, use `createInitialBoard()`, not `originalBoard`.
- If rotation resets a board or mode, check that all four state declarations
  use `rememberSaveable` and that the Activity is allowed to recreate normally.

## Run / test

Build and run from Android Studio, preferably using the course's Android 15 /
API 35 device. Use portrait orientation for interacting with all controls.

1. Verify the initial puzzle, number pad, Solve, and Reset appear. Select the
   empty cell at row 0, column 2 (index 2), and enter **4**.
2. Rotate to landscape and back. Verify the 4 and selected cell survive. The
   existing Activity lifecycle logs should show recreation.
3. Tap Solve. Verify the completed board, bold accepted clues (including your
   4), accent-colored generated digits, Edit clues, and Reset. The number pad
   and Solve button should disappear; selection should be cleared.
4. Tap several solved cells. Verify no selection or `Cell clicked` logs occur.
5. Rotate to landscape and back again. Verify the completed board and Solution
   mode survive, including the clue/generated-digit distinction.
6. Tap Edit clues. Verify your 4 remains, generated digits disappear, and the
   number pad returns. Clear the 4; both displayed and accepted clues update.
7. Enter **7** at index 2 to create a row conflict, then tap Solve. Verify it
   stays in Edit mode and logs rejection. Reset should remove the conflict,
   restore the 30 built-in clues, and clear selection.
8. Enter **1** at index 2. This has no immediate duplicate but makes the puzzle
   unsolvable. Solve should leave the board editable and log no solution.
9. Reset, solve successfully, then Reset directly from Solution mode. Verify
   the original puzzle and editing controls return.

**Known layout limitation:** the board uses width-based square sizing. Landscape
can crowd or clip the controls. Rotate back to portrait to inspect and interact
with them. The rotation requirement here is state preservation; the layout is
unchanged apart from the action-button row.

The domain algorithms are unchanged. Their existing local JVM tests remain
`SudokuValidatorTest` and `SudokuSolverTest`; they do not test Compose controls or
Activity recreation. Validate those behaviors with the checks above.

## Expected result

- ✓ Successful solving enters a read-only Solution mode.
- ✓ Edit clues restores only accepted clues, including manual edits.
- ✓ Reset restores the built-in puzzle from either mode.
- ✓ Invalid and unsolvable puzzles remain editable.
- ✓ Board, clues, selection, and mode survive normal rotation.
- ✓ Logcat records meaningful actions and mode transitions.

## Architecture after this step

```text
MainActivity
  └── SudokuScreen (saved board, clues, selection, mode)
        ├── SudokuBoard → SudokuCell (interaction enabled in EDIT)
        ├── NumberPad (EDIT only)
        ├── Solve / Edit clues, and Reset
        ├── SudokuValidator
        └── SudokuSolver
```

The screen still owns behavior. The board and number pad receive values and
callbacks, and the domain functions remain pure Kotlin.
