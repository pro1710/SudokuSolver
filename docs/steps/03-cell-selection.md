# Step 03 — Cell selection

## Goal

Tap any Sudoku cell to highlight it. Tapping another cell moves the highlight,
so at most one cell is selected. The puzzle digits stay unchanged.

## Starting point

Start from Step 02, commit `635bce0` (`step-02: add static sudoku board`).
`SudokuBoard.kt` contains the static puzzle, a Column of nine Rows, 81
`SudokuCell` calls, and a Canvas that draws the grid over the cells.
`MainActivity` already places this board below the title.

All code changes in this step belong in `SudokuBoard.kt`.

## Concepts introduced

- **State** is information that can change while the app is running. Here it
  is the index of the selected cell, not the puzzle's digits.
- **`Int?`** is a nullable integer: it holds either an index or `null`.
  `null` means no selection. Using `0` for that purpose would be incorrect,
  because index zero is the top-left cell.
- **`mutableStateOf`** creates state that Compose observes. Changing its value
  tells Compose that UI which reads it may need updating.
- **Recomposition** is Compose running affected composable code again to
  describe the UI after state changes. We describe whether a cell is selected;
  we do not find an existing view and manually repaint it.
- **`remember`** retains a value across recompositions while its call remains
  in the composition. It does not preserve selection after Activity recreation,
  such as a normal configuration change, or after the app process is lost.
- **`by` delegation** lets Kotlin read and write the state's value using
  `selectedCell` instead of explicitly using `.value`.
- **Callbacks** let a child report an event to its parent. The cell reports a
  click; the board decides which index to store.

## Implementation

### 1. Add one selection state to the board

Add these imports:

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
```

Inside `SudokuBoard`, after the existing array declaration and before
`lineColor`, add:

```kotlin
var selectedCell by remember {
    mutableStateOf<Int?>(null)
}
```

On first composition, the state starts with no selection. `remember` returns
the same state holder on subsequent recompositions. An ordinary local
`var selectedCell: Int? = null` would neither notify Compose of changes nor
retain its value when the function runs again.

Keep this state outside the row/column loops. A single selected index ensures
one selection for the entire board; independent selected flags in each cell
would allow several cells to remain highlighted.

### 2. Give the cell selection and click parameters

Change the `SudokuCell` signature to:

```kotlin
fun SudokuCell(
    value: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

Keep its `@Composable` annotation and body. `isSelected` describes what to
draw. `onClick: () -> Unit` is a function parameter: it accepts no arguments
and returns no useful value. The cell can invoke it without knowing the
board's array index or owning selection state.

### 3. Connect each cell to the board's state

In the inner loop, keep `val index = row * 9 + column`. Add two arguments to
the existing cell call:

```kotlin
SudokuCell(
    value = board[index],
    isSelected = selectedCell == index,
    onClick = { selectedCell = index },
    modifier = Modifier
        .weight(1f)
        .fillMaxSize()
)
```

The comparison is true only for the selected index. The lambda in braces
captures this particular cell's index and assigns it when clicked. Passing
a callback does not run it during composition.

For example, tapping row `2`, column `5` stores index `23`. Compose can then
update cells that read the selection: cell 23 receives `isSelected = true`,
and the previously selected cell receives `false`. Tapping the selected cell
again keeps it selected; this implementation does not toggle selection off.

### 4. Draw and activate the selection

At the start of `SudokuCell`'s body, before its Box, choose the colors:

```kotlin
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
```

Material supplies matching background/text color pairs for light and dark
themes. The exact highlight color depends on the theme and device.

Replace the Box's `modifier = modifier` argument with:

```kotlin
modifier = modifier
    .background(backgroundColor)
    .clickable(onClick = onClick),
```

The caller's sizing modifiers still apply. The background fills the cell, and
`clickable` makes that entire area respond, including empty cells. Attach it
to the Box rather than just the Text so tapping away from a digit also works.

In the existing Text call, replace its color argument with `color = textColor`.
Keep the zero-to-empty-string rule, typography, and centered alignment.

The Canvas stays unchanged and draws the grid above the cell backgrounds.
It has no input handler, so it does not consume the cells' clicks.

## Logging / troubleshooting

This step adds no custom logs. Step 03A will introduce event and lifecycle
logging; for now, observe the persistent background highlight on the device.

- **Delegation error mentioning `getValue` or `setValue`:** add both Compose
  runtime imports shown in section 1.
- **Tap ripple appears but selection does not remain:** check that
  `selectedCell` uses `remember` and `mutableStateOf`, and that the callback
  assigns `index` to it. A ripple is temporary click feedback, not the
  selection background.
- **Multiple cells remain selected:** store one nullable index in the board,
  rather than a separate Boolean state in each cell.
- **Empty cells do not respond:** put `clickable` on the full-size Box.
- **Preview does not respond to taps:** use Android Studio's interactive
  preview mode or run the app; a static preview only shows the initial state.
- **Rotation clears selection:** this is expected with `remember` at this
  stage. It retains state through recomposition, not Activity recreation.

## Run / test

In Android Studio:

1. Build and run the app on the Android 15 / API 35 virtual device in portrait.
2. On a fresh launch, confirm the original puzzle appears with no selection.
3. Tap the top-left `5`: its whole cell should stay highlighted after the
   click ripple finishes.
4. Tap an empty cell, then the bottom-right `9`. Each tap should move the
   highlight and remove it from the previous cell.
5. Tap near the edge of a cell, away from its digit. The cell should still
   respond. Tap the same cell again and confirm it remains selected.
6. Confirm the digits never change and all grid boundaries remain visible.
7. Repeat a selection in light and dark appearance to check text contrast.
8. Rotate the device and return to portrait. With normal Activity recreation,
   selection resets; the static puzzle stays the same. Landscape layout
   remains the width-based layout from Step 02.

There are no new unit tests for this small UI interaction. The instructor's
build and tap checks validate this step before its commit is approved.

## Expected result

- ✓ No cell is initially selected.
- ✓ Both filled and empty cells can be selected.
- ✓ Exactly one cell is highlighted after a tap.
- ✓ The highlight moves without changing any puzzle digits.
- ✓ Grid lines and text remain readable.

## Architecture after this step

```text
MainActivity
    └── SudokuBoard
        ├── static puzzle: IntArray
        ├── remembered state: selectedCell: Int?
        ├── SudokuCell × 81
        │       ↓ receives value and isSelected
        │       ↑ reports onClick to the board
        └── Canvas grid overlay
```

The board owns the selection. Each cell renders the supplied values and
reports clicks through a callback, keeping the selection decision in one place.
