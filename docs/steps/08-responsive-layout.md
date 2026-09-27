# Step 08 — Support portrait and landscape layouts

## Goal

Show the board above the controls in a tall content area and beside them in a
wide content area. Keep the board square and within its available space. Keep Solve/Edit clues and Reset visible, and let the number pad scroll when
height is limited. Remove the screen title to free space while retaining the current
puzzle, selection, conflicts, and mode during rotation.

## Starting point

Start from Step 07D, commit `1b2248b`
(`step-07d: clean architecture and test viewmodel`). The app has a tested domain
layer and ViewModel, but its single vertical layout is awkward in landscape.
The grid forces the full width before making itself square, leaving too little
height for the controls.

This is the first extension after the original sequence through Step 07D.
Earlier tutorials retain their historical landscape limitation; this commit
addresses it through layout changes in `SudokuScreen.kt` and `SudokuGrid.kt`.

## Concepts introduced

- **Constraints** describe the minimum and maximum dimensions a parent offers
  a child. Screen dimensions alone are not enough: system insets and padding consume space too.
- **`BoxWithConstraints`** exposes available dimensions such as `maxWidth` and
  `maxHeight` so composition can choose an appropriate arrangement.
- **Bounded areas and weights** allocate room for the board and controls. An
  inner `Box` can center a square board within a rectangular area.
- **Modifier order** affects sizing. Forcing full width before requesting a
  square can prevent a board from respecting a short height constraint.
- **`verticalScroll` and `rememberScrollState`** let the number pad exceed
  its viewport's height while keeping the action row visible.

Read Android's [different display sizes guide](https://developer.android.com/develop/ui/compose/layouts/adaptive/support-different-display-sizes)
for decisions based on available space, and its
[constraints and modifier order guide](https://developer.android.com/develop/ui/compose/layouts/constraints-modifiers)
for parent-to-child measurement.

## Implementation

### 1. Keep the state owner outside the layout choice

Leave `SudokuScreen` and its connection to `SudokuViewModel` unchanged.
`SudokuScreenContent` still receives one `uiState` and the same six callbacks.
Changing between a Row and a Column must not create another board or ViewModel.

Extract two small private composables into `SudokuScreen.kt` so both arrangements
render the same content:

- `SudokuBoardPane(uiState, onCellSelected, modifier)` wraps the existing grid
  call in `Box(modifier = modifier, contentAlignment = Alignment.Center)`.
- `SudokuControls(mode, onNumberClick, onClearClick, onSolveClick,
  onEditCluesClick, onResetClick, modifier)` wraps the existing number pad and
  button Row in a Column with `Arrangement.spacedBy(16.dp)`.

Copy the existing values and callbacks into these helpers without changing
their meaning. The control helper uses `mode` in place of `uiState.mode`:
EDIT shows the number pad and Solve, SOLUTION shows Edit clues, and both show
Reset. The board helper still disables editing in SOLUTION.

Import `Box` and `BoxWithConstraints` from `androidx.compose.foundation.layout`,
plus `androidx.compose.ui.Alignment`.

### 2. Remove the title and measure the available content area

Remove the `SudokuTitle()` call and delete `SudokuTitle` and its preview. The
screen now uses that space for the board and controls. Replace the outer title
Column with a full-size `BoxWithConstraints`:

```kotlin
BoxWithConstraints(modifier = modifier.fillMaxSize().padding(16.dp)) {
    val controlsMaxHeight = maxHeight * 0.45f
    if (maxWidth > maxHeight) {
        // Put the board and controls in a Row.
    } else {
        // Put the board and controls in a Column.
    }
}
```

The modifier passed from `MainActivity` already includes Scaffold's inset
padding, so these dimensions describe the usable content area after padding.

This is a small, content-specific rule: use two columns when the available area
is wider than it is tall. A narrow app window can use the stacked layout even
on a physically landscape device. No orientation listener or manifest setting
is needed, and the choice need not be saved in the ViewModel.

Add imports for `fillMaxSize`, `fillMaxHeight`, and `heightIn` from the layout
package, plus `rememberScrollState` and `verticalScroll` from
`androidx.compose.foundation`.

### 3. Give the wide layout two bounded areas

In the wide branch, use a full-size Row with 16 dp spacing. Give the board and
controls equal shares of its width:

```kotlin
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
```

The board's area has both width and height limits. Its centered square can use
whichever dimension is smaller. The controls have a finite height. Inside them,
the action row reserves its own space and only the number pad can scroll.

### 4. Keep the tall layout stacked

In the other branch, use a full-size Column with 16 dp spacing. Give
`SudokuBoardPane` the modifier `Modifier.weight(1f).fillMaxWidth()`.
Call the same control helper with the same callbacks, using:

```kotlin
modifier = Modifier
    .fillMaxWidth()
    .heightIn(max = controlsMaxHeight)
```

The unweighted control area takes its natural height up to 45% of this content
area. The weighted board area gets the remaining height after spacing. The
45% cap is a layout choice for this lesson, not an Android rule: it reserves
space for the board when the number pad needs more height. In Solution mode, the
shorter controls naturally leave more room for the board.

Capture `controlsMaxHeight` in the `BoxWithConstraints` scope before entering
the nested Column. Compose's layout scopes restrict implicit access to an outer
receiver; using this local value avoids a scope-resolution compiler error.

Inside `SudokuControls`, give only `NumberPad` a scroll modifier and a flexible
height. Keep the Solve/Edit clues and Reset Row outside this scroll area:

```kotlin
NumberPad(
    onNumberClick = onNumberClick,
    onClearClick = onClearClick,
    modifier = Modifier
        .weight(1f, fill = false)
        .verticalScroll(rememberScrollState())
)
```

A Column measures its unweighted children first. The action Row therefore gets
its required height, and the weighted number pad receives the remaining space
after the 16 dp gap. `fill = false` allows the pad to use less height when all
its buttons fit; when they do not fit, its contents scroll within that space.
Solve/Edit clues and Reset stay visible while the pad scrolls.

Do not put `verticalScroll` on the entire control Column or on the outer
constraint container. Scrolling the whole control Column would allow the action
row to disappear below the number pad. See Android's
[scroll modifier guide](https://developer.android.com/develop/ui/compose/touch-input/scroll/scroll-modifiers).

The scroll position is local UI state. Board values and mode remain in the
ViewModel regardless of which branch is composed.

### 5. Let the grid fit both dimensions

In `SudokuGrid.kt`, remove `.fillMaxWidth()` before `.aspectRatio(1f)` and remove
its unused import. The outer grid Box now starts with:

```kotlin
modifier = modifier
    .aspectRatio(1f)
    .background(MaterialTheme.colorScheme.surface)
```

`aspectRatio(1f)` can now choose a square that fits the parent's width and height
limits. Previously `fillMaxWidth` made full width a requirement before the square
was measured. The board area's Box does not pass its exact minimum size down to
the grid, so the grid can be smaller and centered.

Keep the cells, digit styles, click behavior, and grid-line calculation. Make
the Canvas safe even if resizing gives it zero or extremely little space:

```kotlin
if (size.minDimension <= 0f) return@Canvas
```

Put this before calculating cell dimensions. Inside the line loop, clamp the
stroke width before using the existing border-coordinate clamping:

```kotlin
val desiredStrokeWidth = if (line % 3 == 0) 3.dp.toPx() else 1.dp.toPx()
val strokeWidth = desiredStrokeWidth.coerceAtMost(size.minDimension)
```

This prevents an invalid clamp interval when the drawing area is narrower than
a normal line. Thin and thick lines retain their previous sizes at normal board
dimensions. A tiny viewport cannot make the board comfortably readable, but it
should not cause a drawing exception.

### 6. Preview specific sizes

Give the standalone grid preview an explicit 360 × 360 dp size. Replace the
single screen preview annotation with three annotations on the same preview
function:

```kotlin
@Preview(name = "Portrait", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Landscape", showBackground = true, widthDp = 800, heightDp = 360)
@Preview(
    name = "Small landscape, large text",
    showBackground = true,
    widthDp = 640,
    heightDp = 320,
    fontScale = 1.5f
)
```

The previews reuse the existing sample `SudokuUiState` and empty callbacks.
They show the arrangements without constructing a ViewModel. Runtime insets
consume some space that a content-only preview does not, so still check a device.

## Logging / troubleshooting

There are no new layout logs. Resizing can trigger composition frequently, so
logging every measurement would obscure useful events. Existing tags still
describe clicks, actions, validation, and Activity lifecycle events:

```text
D/SudokuGrid: Cell clicked: index=2, row=0, column=2, value=0
D/SudokuViewModel: Selected cell changed: index=2
D/SudokuViewModel: Cell value changed: index=2, from=0, to=4
```

Normal rotation may recreate the Activity, but it should not create a new
ViewModel or clear the current puzzle. Filter Logcat by `MainActivity` and
`SudokuViewModel` to compare those lifetimes.

- If the grid is clipped in landscape, check that its old `fillMaxWidth()` was
  removed and that it sits inside the bounded board area.
- If scrolling reports an infinite-height constraint, check that the control
  Column has a bounded height and only its weighted number pad scrolls.
- If Solve or Reset disappears, verify the action Row is outside the scroll
  modifier and the number pad uses `weight(1f, fill = false)`.
- If values reset when the layout switches, check that both branches use the
  supplied `uiState` and callbacks, rather than creating state themselves.

## Run / test

1. Build in Android Studio and inspect all three screen previews and the square
   grid preview. Verify a square board, 3×3 boundaries, and the intended arrangement.
2. Run on the course's Android 15 / API 35 phone. In portrait, verify the board
   is above the number pad and actions; in landscape, verify it is beside them.
   The title should be absent and content should respect the system bars.
3. In portrait select index 2 and enter **4**. Rotate both ways. Verify the digit
   and selected highlight remain, and that the grid remains entirely visible.
4. Clear that cell and enter **7** to create a conflict with index 4. Rotate
   again. Verify both conflict highlights remain and Solve still rejects it.
5. Reset, enter **4** at index 2, and solve. Rotate in SOLUTION mode. Verify clue
   styling, the completed board, disabled cell taps, and the hidden number pad.
6. Tap Edit clues in landscape. Verify your 4 remains, generated digits disappear,
   and the number pad returns. Verify Clear and Reset work in both arrangements.
7. Use a smaller window or larger font setting in Android Studio's device.
   Verify Solve/Edit clues and Reset remain visible without scrolling. Scroll
   the number pad to reach every digit and Clear when necessary. Check that the
   board and action row stay in place while the number pad scrolls.
8. Restore the normal size/text settings. Verify the controls fit naturally
   when space permits and the board resizes without resetting state.

The existing 47 Sudoku JVM tests (48 including the generated example) still
check domain and ViewModel behavior. They do not measure or render this layout;
previews and device checks provide the visual validation for this step.

## Expected result

- ✓ Tall content areas show the board above the controls.
- ✓ Wide content areas show the board beside the controls.
- ✓ The board stays square and fits its allocated width and height.
- ✓ Solve/Edit clues and Reset stay visible; the number pad can scroll when needed.
- ✓ The screen title is removed to give the puzzle more space.
- ✓ Selection, edits, conflicts, clues, and mode survive normal rotation.
- ✓ Existing solver behavior, callbacks, and logging remain intact.

## Architecture after this step

```text
SudokuScreen → existing SudokuViewModel
  └── SudokuScreenContent → BoxWithConstraints
        └── Row or Column
              ├── SudokuBoardPane → SudokuGrid → SudokuCell
              └── SudokuControls
                    ├── NumberPad (scrolls if needed)
                    └── Solve/Edit clues and Reset (remain visible)
```

Only the presentation arrangement changes. The same state flows into either
layout, and events go back to the same ViewModel. Its configuration-change
lifetime and the existing domain APIs remain unchanged.
