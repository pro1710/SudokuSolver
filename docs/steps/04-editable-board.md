# Step 04 — Editable board and number pad

## Goal

Select a cell, enter a digit with a 3×3 number pad, or press **Clear** to empty
it. All cells are editable, including the built-in clues. The board accepts
duplicate digits at this stage; validation will be a separate step.

## Starting point

Start from Step 03A, commit `d805911`
(`step-03a: add logging and lifecycle diagnostics`). `SudokuBoard` contains
the puzzle, remembered selection, and selection effect. `MainActivity` draws
the title and board and logs its lifecycle.

This step adds `SudokuScreen.kt` and `NumberPad.kt` beside the existing Kotlin
files in package `com.example.sudokusolver`. Keep the existing theme, SDK
settings, dependencies, cell rendering, and grid drawing.

## Concepts introduced

**State hoisting** means moving state to a parent that coordinates the
components using it. The board and number pad must agree on the selected
cell and current digits, so their common parent, `SudokuScreen`, owns both.
The board receives data and reports selection events; the pad reports number
and Clear events.

This is **unidirectional data flow**:

```text
state ↓
      UI
events ↑
```

The screen applies an event to state; Compose then renders the new values.
There is one source of truth rather than separate arrays in different children.
See Android's [Where to hoist state](https://developer.android.com/develop/ui/compose/state-hoisting)
guide, focusing on the section about hoisting state within composables for
this step.

**Immutable-style updates** replace the current array with a changed copy.
An `IntArray` itself is mutable, but we treat the published array as a snapshot
and only mutate a fresh copy before assigning it to state.

**Callbacks with arguments**, such as `(Int) -> Unit`, let a child report a
selected index or pressed digit. A `() -> Unit` callback reports Clear without
needing an argument. The Material `Button` component invokes its `onClick`
callback when pressed.

## Implementation

### 1. Move state into a new screen

Create `SudokuScreen.kt` and add
`@Composable fun SudokuScreen(modifier: Modifier = Modifier)`.
Move the puzzle from `SudokuBoard` into this function, retaining all 81 values.
Turn the former `val board` into observable state:

```kotlin
var board by remember {
    mutableStateOf(
        intArrayOf(
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
    )
}
```

Move the existing `selectedCell` declaration and effect into the screen too.
Change the effect's tag to reflect its new owner:

```kotlin
var selectedCell by remember {
    mutableStateOf<Int?>(null)
}

LaunchedEffect(selectedCell) {
    Log.d("SudokuScreen", "Selected cell changed: index=$selectedCell")
}
```

Move the corresponding runtime imports (`LaunchedEffect`, `getValue`,
`mutableStateOf`, `remember`, and `setValue`) from the board file to the screen.
The screen also needs `android.util.Log`, `Composable`, and `Modifier`.
Android's [Side-effects in Compose](https://developer.android.com/develop/ui/compose/side-effects)
guide explains how `LaunchedEffect` runs and restarts when its keys change.

### 2. Make the board receive state and report selection

Replace the `SudokuBoard` signature with:

```kotlin
fun SudokuBoard(
    board: IntArray,
    selectedCell: Int?,
    onCellSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
)
```

Keep `@Composable`. Remove the old local puzzle, selection state, and effect
now that they live in the screen. Inside the existing click callback, preserve
the `Cell clicked` log but replace `selectedCell = index` with:

```kotlin
onCellSelected(index)
```

The child now asks its parent to select a cell. It does not assign to the
received `selectedCell` or mutate the received array. Keep the Canvas and
`SudokuCell` exactly as they were.

### 3. Create the number pad

Create `NumberPad.kt` in the same package. Its composable has this signature:

```kotlin
@Composable
fun NumberPad(
    onNumberClick: (Int) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

Inside it, arrange the nine number buttons:

```kotlin
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
}
```

The formula produces rows `1 2 3`, `4 5 6`, and `7 8 9`. As with the board,
equal weights divide each row evenly. The callback carries the digit to the
parent; the pad knows nothing about cell indices.

After the loop, still inside the Column, add the full-width Clear button:

```kotlin
Button(
    onClick = onClearClick,
    modifier = Modifier.fillMaxWidth()
) {
    Text("Clear")
}
```

Use imports from `androidx.compose.foundation.layout` for `Arrangement`,
`Column`, `Row`, and `fillMaxWidth`; from `androidx.compose.material3` for
`Button` and `Text`; and the usual `Composable`, `Modifier`, and `unit.dp`.
The official [Button guide](https://developer.android.com/develop/ui/compose/components/button)
describes the `onClick` and `content` parameters used by these buttons.

### 4. Assemble the screen and handle edits

After the screen's state and effect, add the following Column. Its title,
spacing, and margin move from the Activity; its callbacks coordinate the two
children:

```kotlin
Column(
    modifier = modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    SudokuTitle()
    SudokuBoard(
        board = board,
        selectedCell = selectedCell,
        onCellSelected = { index -> selectedCell = index }
    )
    NumberPad(
        onNumberClick = { number ->
            val index = selectedCell
            if (index == null) {
                Log.w("SudokuScreen", "Number ignored: number=$number, no cell selected")
            } else {
                val previousValue = board[index]
                val newBoard = board.copyOf()
                newBoard[index] = number
                board = newBoard
                Log.d(
                    "SudokuScreen",
                    "Cell value changed: index=$index, from=$previousValue, to=$number"
                )
            }
        },
        onClearClick = {
            val index = selectedCell
            if (index == null) {
                Log.w("SudokuScreen", "Clear ignored: no cell selected")
            } else {
                val previousValue = board[index]
                val newBoard = board.copyOf()
                newBoard[index] = 0
                board = newBoard
                Log.d("SudokuScreen", "Cell cleared: index=$index, previousValue=$previousValue")
            }
        }
    )
}
```

Add `Arrangement`, `Column`, `padding`, and `dp` imports to the screen file.
The local `val index` reads the current selection once per button event.
After the null check, Kotlin knows this local value is a usable integer.
With no selected cell, the handler logs a recoverable warning and leaves
state alone. Buttons remain enabled so students can observe this case.

Both successful handlers copy before writing. Clear uses zero, which the
existing cell composable already displays as blank. Selection stays on the
same cell after either action. Entering its existing value or clearing an
already empty cell is harmless and still logs the accepted action.

Why not simply write this?

```kotlin
board[index] = number
```

Compose observes the `MutableState` holding the array, not arbitrary writes
to individual `IntArray` elements. Changing an element does not call the
state holder's setter, so it does not reliably trigger a UI update. An
unrelated recomposition might reveal the mutated value later, which can make
the mistake confusing. Assigning the same array back is also insufficient.
Creating and assigning a new array gives Compose a new state value to observe.
Read the **State in composables** section of
[State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state)
for `remember`, `mutableStateOf`, and the warning about non-observable mutable
objects producing stale UI.

### 5. Connect the Activity and update previews

Replace the Activity's Column inside `Scaffold` with:

```kotlin
SudokuScreen(modifier = Modifier.padding(innerPadding))
```

Remove its unused `Arrangement`, `Column`, and `dp` imports. Keep
`SudokuTitle`, its preview, the theme, and all lifecycle logs. Scaffold padding
is still passed in by the Activity, and the screen adds the content margin.

The board now requires arguments. Update its preview call to:

```kotlin
SudokuBoard(
    board = IntArray(81),
    selectedCell = 0,
    onCellSelected = {}
)
```

This deliberately renders an empty board with the first cell selected; its
empty callback means the component preview does not change selection.

Add themed previews to the new files, using the existing preview pattern:

```kotlin
@Preview(showBackground = true)
@Composable
fun NumberPadPreview() {
    SudokuSolverTheme {
        NumberPad(onNumberClick = {}, onClearClick = {})
    }
}
```

```kotlin
@Preview(showBackground = true)
@Composable
fun SudokuScreenPreview() {
    SudokuSolverTheme {
        SudokuScreen()
    }
}
```

Import `Preview` and `SudokuSolverTheme` in each new file. Use the screen
preview in interactive mode, or the running app, to test coordinated editing.

## Logging / troubleshooting

Filter Logcat with `package:com.example.sudokusolver level:DEBUG` to see both
board clicks and screen actions, including warnings. A `tag:SudokuScreen`
filter narrows the output to the state owner.

Before selecting a cell, pressing `4` and Clear produces:

```text
W/SudokuScreen: Number ignored: number=4, no cell selected
W/SudokuScreen: Clear ignored: no cell selected
```

Select index 23, enter `4`, then clear it:

```text
D/SudokuBoard: Cell clicked: index=23, row=2, column=5, value=0
D/SudokuScreen: Selected cell changed: index=23
D/SudokuScreen: Cell value changed: index=23, from=0, to=4
D/SudokuScreen: Cell cleared: index=23, previousValue=4
```

The selection effect moved with its state, so its tag is now `SudokuScreen`.
Entering a number does not change the selected index and therefore does not
restart that effect. The next board-click log reads the current edited value.

- **Emulator shows a black screen:** first inspect Logcat for a crash. If the
  Activity reaches `onResume` and the screen's initial selection log appears
  without a fatal exception, stop the emulator and try **Device Manager →
  device menu → Cold Boot**, then run the app again. This resolved the black
  screen observed on the instructor's Pixel 9 API 37.2 emulator during this
  step, without an app code change. Those log messages alone do not prove that
  the UI rendered correctly. Android's
  [emulator snapshots guide](https://developer.android.com/studio/run/emulator-snapshots)
  explains Quick Boot snapshots and how to perform a cold boot.
- **Edits appear only after another tap:** ensure each handler uses `copyOf()`
  and assigns `board = newBoard`; do not mutate the published array.
- **Pad cannot access the selected cell:** keep both state variables in the
  screen and use callbacks, rather than restoring private state in the board.
- **Missing parameters in a preview:** update the board preview as above.
- **Clear displays zero:** keep the cell's existing zero-to-empty-string rule.
- **Edits disappear after rotation:** both states still use `remember`.
  They survive recomposition, but not normal Activity recreation. State
  preservation is addressed later with the ViewModel step.
- **Controls do not fit on a small or landscape device:** this remains a
  simple vertical layout with a width-based square board. Use a portrait
  device with enough height for this step; adaptive layout is deferred.

## Run / test

In Android Studio:

1. Build and run on the Android 15 / API 35 device in portrait. Confirm the
   title, original puzzle, number pad, and Clear button appear.
2. Before selecting a cell, press a digit and Clear. Check the warning logs
   and confirm no puzzle value changes.
3. Select the empty index 23 (third row, sixth column visually), enter `4`,
   then enter `2`. The same cell should immediately show each new digit.
4. Press Clear. Only that cell becomes blank and it remains selected. Press
   Clear again to check that clearing an empty cell is harmless.
5. Select the top-left `5`, replace it with `9`, and clear it. Built-in digits
   are editable too at this stage.
6. Select another cell and try all nine buttons to verify their digit labels
   and callbacks agree. Check that other cells retain their values.
7. Verify the sample logs, highlight contrast, and grid visibility. Returning
   from Home should keep edits if the same Activity instance is retained;
   normal rotation/recreation resets the puzzle and selection.

There are no new unit tests for these small Compose event handlers. The
instructor validates the build, immediate UI updates, and Logcat output.

## Expected result

- ✓ The screen owns board and selection state.
- ✓ The number pad displays `1 2 3`, `4 5 6`, `7 8 9`, and Clear.
- ✓ Number entry changes only the selected cell, immediately.
- ✓ Clear empties only the selected cell.
- ✓ Buttons without a selection leave the board unchanged and log warnings.
- ✓ Cell selection and Activity lifecycle diagnostics continue to work.

## Architecture after this step

```text
MainActivity → Scaffold
    └── SudokuScreen (board and selectedCell state)
        ├── SudokuTitle
        ├── SudokuBoard
        │   ├── receives board and selectedCell
        │   └── reports onCellSelected(index)
        └── NumberPad
            ├── reports onNumberClick(number)
            └── reports onClearClick()
```

The screen changes state in response to callbacks. The board and pad describe
the UI and report user actions; neither owns a competing copy of app state.
