# Step 02 — Static 9×9 Sudoku board

## Goal

Display the supplied Sudoku puzzle below the existing **Sudoku Solver** title.
Draw 81 equally sized cells, centered digits, thin cell boundaries, and thicker
3×3 boundaries. Empty cells display no text. The board is static.

## Starting point

Start from Step 01, commit `f71137f` (`step-01: simplify compose project`).
`MainActivity.kt` displays `SudokuTitle` inside a themed `Scaffold` and has a
title preview. Keep that title, the theme, and the existing SDK/dependency
settings.

## Concepts introduced

- **`IntArray`** stores integers. Our board contains 81 entries: `0` means
  empty, and `1..9` are Sudoku digits. `IntArray(81)` creates 81 zeroes;
  `intArrayOf(...)` creates the same array type with explicit starting values.
- **Row-major indexing** stores one row after another in a flat array. With
  zero-based coordinates, `index = row * 9 + column`. For example, row `2`,
  column `5` is index `23`. Array positions run from `0` to `80`.
- **`Column` and `Row`** place children vertically and horizontally.
- **`Box`** can align content and stack children. A cell's Box centers its
  text; the board's Box stacks grid lines over the cells.
- **`Modifier.weight(1f)`** gives siblings equal shares of available space.
  Each row receives one ninth of the board's height; each cell receives one
  ninth of its row's width. Weight belongs to the parent layout's scope: rows
  use it inside a Column, and cells use it inside a Row.
- **`aspectRatio(1f)`** requests equal width and height. With `fillMaxWidth()`,
  the board uses the available width to make a square when space allows.
- **`Canvas`** draws shapes using pixel coordinates. `dp` describes
  density-independent sizes; `toPx()` converts line thickness to pixels for
  drawing. `Offset(x, y)` represents a point, measured from the top-left.

## Implementation

### 1. Create a board composable and its data

Create `SudokuBoard.kt` beside `MainActivity.kt`, in package
`com.example.sudokusolver`. Keep the board UI in this file; we do not need a
domain model or another state-owning component yet.

Start with `@Composable fun SudokuBoard(modifier: Modifier = Modifier)`.
Inside it, declare the puzzle:

```kotlin
val board: IntArray = intArrayOf(
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
```

The line breaks help us read the puzzle, but the array is still one-dimensional.
There are nine groups of nine integers. `val` prevents reassigning the variable;
it does not make the array's contents immutable. This step never modifies them.

### 2. Build one cell

Add this composable below the board function:

```kotlin
@Composable
fun SudokuCell(value: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (value == 0) "" else value.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
```

The caller decides the cell size through its modifier. The Box centers the
text within that size. An empty string hides a zero without removing the cell
from the layout. The theme supplies text size and color.

### 3. Arrange nine rows of nine cells

After the array declaration inside `SudokuBoard`, add a square container:

```kotlin
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
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}
```

`0 until 9` includes `0` through `8`, so each loop has nine iterations. The outer
loop chooses the row; the inner loop chooses the column. Multiplying the row
by nine skips the entries in all earlier rows.

The Column fills the square. Row weights divide its height equally. Cell
weights divide the row width equally, and `fillMaxSize()` fills each cell's
allocated space, including the row height. We let the parent layout size the
cells instead of hard-coding a pixel size.

### 4. Draw the grid above the cells

Before the board's outer Box, capture the line color:

```kotlin
val lineColor = MaterialTheme.colorScheme.onSurface
```

The Canvas drawing block is not a composable context, so read the theme in the
composable function and use the resulting color when drawing.

Inside the outer Box, **after** the Column, add:

```kotlin
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
```

Unlike `until`, `0..9` includes both endpoints. Nine cells need ten boundary
lines in each direction. `%` computes the remainder: boundaries `0`, `3`, `6`,
and `9` have remainder zero when divided by three, so they get thicker strokes.

`matchParentSize()` makes this Box child cover the board without determining
the Box's size. A later child draws over earlier children, so the lines appear
above the cell content. Drawing each boundary once avoids doubled borders
between neighboring cells.

A stroke extends half its thickness to each side of its coordinate. `coerceIn`
limits coordinates to the supplied range, moving the outer borders inward by
half a stroke so their full thickness stays inside the board. Interior lines
stay at their calculated positions.

Let Android Studio add imports for the new symbols. The main groups are
`androidx.compose.foundation` (`Canvas`, `background`),
`androidx.compose.foundation.layout` (layouts and sizing modifiers),
`androidx.compose.material3` (`MaterialTheme`, `Text`), and Compose UI types
(`Alignment`, `Modifier`, `geometry.Offset`, `unit.dp`). `weight` and
`matchParentSize` are available in their Row/Column and Box receiver scopes.

### 5. Place the board below the existing title

In `MainActivity.onCreate`, replace only the `SudokuTitle(...)` call inside
the Scaffold's content lambda with:

```kotlin
Column(
    modifier = Modifier
        .padding(innerPadding)
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    SudokuTitle()
    SudokuBoard()
}
```

Add imports for `Arrangement`, `Column`, and `dp`. The first padding respects
the Scaffold's system-bar spacing; the second adds a margin around our content.
`spacedBy(16.dp)` separates the title from the board. Keep the title composable,
its preview, and the theme as they were.

### 6. Add a board preview

At the bottom of `SudokuBoard.kt`, add:

```kotlin
@Preview(showBackground = true)
@Composable
fun SudokuBoardPreview() {
    SudokuSolverTheme {
        SudokuBoard()
    }
}
```

Import `Preview` and `com.example.sudokusolver.ui.theme.SudokuSolverTheme`.
This preview renders the board component. Run the app to inspect the title,
outer margin, and system-bar spacing together.

## Logging / troubleshooting

No custom logging is added yet; event and lifecycle logs begin in Step 03A.

- **Zeros are visible:** check the `if (value == 0) ""` branch in `SudokuCell`.
- **Digits are misplaced or repeated:** use `row * 9 + column`, not
  `row + column`, and check all 81 puzzle entries.
- **Index out of bounds:** use `0 until 9` for cell loops. `0..9` is appropriate
  only for the ten grid boundaries.
- **Cells collapse or have unequal sizes:** check the square container, the
  Column's `fillMaxSize()`, row/cell weights, and the cell's `fillMaxSize()`.
- **Theme access fails inside Canvas:** move the `lineColor` declaration
  outside the drawing block as shown above.
- **Grid is missing:** ensure Canvas is inside the board Box, after the Column,
  with `matchParentSize()`.
- **Landscape is cramped:** this first board uses width-based square sizing.
  Use portrait for this step's layout check; responsive layout is deferred.

## Run / test

Perform these checks in Android Studio:

1. Build the app and render `SudokuBoardPreview`.
2. Run on the course's Android 15 / API 35 virtual device in portrait.
3. Confirm the title sits above a square board with nine rows and nine columns.
4. Compare the digits with the puzzle in implementation section 1. In
   particular, the first row displays `5`, `3`, and `7` in columns 1, 2, and 5
   when counting visually from one; the final cell displays `9`.
5. Check that zeros appear blank and all digits are centered.
6. Check thin cell lines, thicker boundaries after every third row/column, and
   a complete thick outer border.
7. Tap both a digit and an empty cell. Nothing should change in this static step.
8. Inspect the app in light and dark appearance: grid lines and digits should
   remain readable against the board's theme surface color.

No new unit tests are needed for this static layout. The generated example
tests remain unchanged. Preview and runtime checks are the relevant validation
for the board's appearance; the instructor approves the Android Studio result.

## Expected result

- ✓ The original title remains above the board.
- ✓ All 81 cells use the supplied puzzle, with zeros hidden.
- ✓ Rows and columns divide the square into equal cells.
- ✓ Thin lines and thick 3×3 boundaries are visible.
- ✓ Tapping cells has no effect.

## Architecture after this step

```text
MainActivity → SudokuSolverTheme → Scaffold
    └── Column
        ├── SudokuTitle
        └── SudokuBoard (local IntArray with 81 entries)
            └── Box
                ├── Column → 9 Rows → 81 SudokuCells
                └── Canvas (grid lines)
```

`SudokuBoard` and `SudokuCell` are UI functions in `SudokuBoard.kt`. Their names
describe what they draw; there is no domain board class in this step.
