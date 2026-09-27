# Step 07A — Move state and actions to a ViewModel

## Goal

Give the Sudoku screen one owner for its state and actions: `SudokuViewModel`.
Compose displays that state and forwards user events. Selecting, editing,
validating, solving, returning to clues, and resetting should behave as before.
The ViewModel retains the current puzzle during normal device rotation.

## Starting point

Start from Step 06C, commit `27ae644` (`step-06c: add edit and solution modes`).
`SudokuScreen` currently owns four saved state variables, computes conflicts,
and implements all action callbacks inside its layout. The board and number
pad already accept values and callbacks, so they need no changes for this refactor.

## Concepts introduced

- A **ViewModel** holds screen state and behavior independently of the Activity's
  current UI instance. Android retains an Activity-scoped ViewModel through
  normal configuration changes such as rotation.
- A **single source of truth** means one owner changes the application state.
  The screen reads `uiState`; it does not keep another editable board locally.
- **State hoisting** moves state to the owner that coordinates related actions.
  Here the screen's state moves into the ViewModel.
- A **data class** groups related values. Its `copy` function creates another
  instance, replacing named properties and retaining the others.
- A **method reference**, such as `viewModel::solve`, supplies a function for a
  later click. It does not execute the method during composition.

Read the official [ViewModel overview](https://developer.android.com/topic/libraries/architecture/viewmodel)
and [state hoisting guide](https://developer.android.com/develop/ui/compose/state-hoisting).
Focus on screen-level state ownership and configuration changes.

## Implementation

### 1. Add Compose's ViewModel integration

In `gradle/libs.versions.toml`, add a library alias under `[libraries]`:

```toml
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
```

Then add this dependency to `app/build.gradle.kts`:

```kotlin
implementation(libs.androidx.lifecycle.viewmodel.compose)
```

It provides the `viewModel()` composable used below. Reuse the existing
Lifecycle version, `2.11.0`; do not change the version declarations. Sync the
project in Android Studio. See the official
[Compose ViewModel integration](https://developer.android.com/develop/ui/compose/libraries#viewmodel).

### 2. Group the screen's state

Move `SudokuMode.kt` into `ui/` and change its package to
`com.example.sudokusolver.ui`. Its two enum values stay the same.

Create `ui/SudokuUiState.kt` in that package:

```kotlin
data class SudokuUiState(
    val board: IntArray = createInitialBoard(),
    val originalBoard: IntArray = board.copyOf(),
    val selectedCell: Int? = null,
    val conflicts: Set<Int> = emptySet(),
    val mode: SudokuMode = SudokuMode.EDIT
)
```

Move the entire private `createInitialBoard()` function, including its unchanged
81 values, from `SudokuScreen.kt` to the bottom of this file. Each
`SudokuUiState()` call now creates fresh arrays for the valid built-in puzzle.
Its empty conflict set describes that initial puzzle.

These defaults are for starting and resetting the screen. When changing a board,
the ViewModel must explicitly compute and supply its new conflicts. Changing
`board` through `copy` does not automatically recalculate other properties.

### 3. Create the state owner

Create `ui/SudokuViewModel.kt`. Import `androidx.lifecycle.ViewModel` and the
Compose runtime `getValue`, `setValue`, and `mutableStateOf` functions:

```kotlin
class SudokuViewModel : ViewModel() {
    var uiState by mutableStateOf(SudokuUiState())
        private set

    init {
        Log.d("SudokuViewModel", "ViewModel created")
    }
}
```

Also import `android.util.Log` and the two existing domain objects as their
actions are moved. `private set` lets Compose read the state while limiting
assignment to this class. `mutableStateOf` still notifies Compose when a new
state value is assigned.

An `IntArray` remains mutable even behind a `val` property. This design relies
on the UI treating arrays as read-only. `data class.copy` is **shallow**: it does
not duplicate the arrays. Keep the explicit `copyOf()` calls before changing
any cell; otherwise old state and new state could share a modified array.

### 4. Move selection and editing out of Compose

Add `selectCell(index: Int)` to the ViewModel:

```kotlin
fun selectCell(index: Int) {
    if (uiState.mode != SudokuMode.EDIT) return
    uiState = uiState.copy(selectedCell = index)
    Log.d("SudokuViewModel", "Selected cell changed: index=$index")
}
```

Only selection changes here; the other properties retain their values. The mode
check keeps the action consistent with the disabled board in Solution mode.

Move the number and Clear callbacks into `enterNumber(number: Int)` and
`clearCell()`. Both change a selected cell, so give them a shared private helper:

```kotlin
fun enterNumber(number: Int) {
    updateSelectedCell(number)
}

fun clearCell() {
    updateSelectedCell(0)
}
```

In `updateSelectedCell(number: Int)`, return if the mode is not `EDIT`. Read
`uiState.selectedCell`; if null, keep the previous Number/Clear warning and
return. Otherwise move the existing copied-array updates into the helper:

```kotlin
val previousValue = uiState.board[index]
val newBoard = uiState.board.copyOf()
newBoard[index] = number
val newOriginalBoard = uiState.originalBoard.copyOf()
newOriginalBoard[index] = number
uiState = uiState.copy(
    board = newBoard,
    originalBoard = newOriginalBoard,
    conflicts = findConflicts(newBoard)
)
```

Keep the event logs, using `SudokuViewModel` as the tag: zero logs a cleared
cell; a digit logs the previous and new values. The public methods receive the
same indices and digits already produced by the board and number pad.

Create a private `findConflicts(board: IntArray): Set<Int>` helper. Call
`SudokuValidator.findConflicts(board)`, log the sorted result and any conflict
count warning, then return the set. Move the former validation-effect logging
here. The validator itself remains independent of Android.

State now changes once per edit: the displayed board, accepted clues, and
conflicts are published together. Do not leave the old `remember(board)`
validation or `LaunchedEffect` logging in the screen.

### 5. Move solving and the mode transitions

Move the existing Solve callback into `solve()`. Read fields through `uiState`
and return immediately outside `EDIT`. Preserve the conflict rejection, clue
count log, and working copy. On success, replace the separate assignments with:

```kotlin
uiState = uiState.copy(
    board = workingBoard,
    selectedCell = null,
    conflicts = findConflicts(workingBoard),
    mode = SudokuMode.SOLUTION
)
```

`originalBoard` stays unchanged because it is omitted from `copy`. Keep the
success and transition logs under the ViewModel tag. If solving returns false,
log the failure and leave `uiState` unchanged.

Move Edit clues into `editClues()`. Return unless the mode is `SOLUTION`, then:

```kotlin
val clueBoard = uiState.originalBoard.copyOf()
uiState = uiState.copy(
    board = clueBoard,
    selectedCell = null,
    conflicts = findConflicts(clueBoard),
    mode = SudokuMode.EDIT
)
```

Keep its mode-transition log. For `reset()`, save the previous mode, assign
`uiState = SudokuUiState()`, and keep the reset log and conditional
`SOLUTION -> EDIT` log. The defaults restore all five fields together.

The solver and validator APIs are unchanged. Solving still runs synchronously
on the UI thread as in Step 06C; moving a function into a ViewModel does not
automatically put its work on a background thread.

### 6. Connect state and callbacks to the layout

In `SudokuScreen.kt`, import the three new `ui` types and
`androidx.lifecycle.viewmodel.compose.viewModel`. Replace the screen's state
declarations and action implementations with this small connection function:

```kotlin
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
```

Here `viewModel::solve` is equivalent to `{ viewModel.solve() }` as a callback.
Reading `viewModel.uiState` during composition observes its Compose state.
There is no need for a second `remember` or `rememberSaveable` copy.

Create `SudokuScreenContent` below this function. Its parameters are:

```kotlin
uiState: SudokuUiState,
onCellSelected: (Int) -> Unit,
onNumberClick: (Int) -> Unit,
onClearClick: () -> Unit,
onSolveClick: () -> Unit,
onEditCluesClick: () -> Unit,
onResetClick: () -> Unit,
modifier: Modifier = Modifier
```

Move the existing `Column` layout into its body. Replace field reads with
`uiState.board`, `uiState.mode`, and so on. Pass the supplied callbacks directly
to the board, number pad, and buttons; for example, the Solve button uses
`onClick = onSolveClick`. Preserve all sizing, padding, and conditional controls.

The content function owns no application state and does not need a ViewModel
parameter. Update `SudokuScreenPreview` to call it with `SudokuUiState()` and
six empty callbacks (`{}`). This gives a static initial-board preview without
creating a ViewModel. Remove the screen's unused state, logging, and domain imports.

`MainActivity` still calls `SudokuScreen` inside its existing Scaffold.
In this app, `viewModel()` retrieves an instance scoped to that Activity.
Do not replace it with `SudokuViewModel()` or `remember { SudokuViewModel() }`:
those would bypass the framework's retention mechanism. See Android's
[ViewModel scoping APIs](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-apis).

## Logging / troubleshooting

Filter Logcat with `package:com.example.sudokusolver tag:SudokuViewModel`.
Entering 4 in the empty cell at index 2, then solving, includes:

```text
D/SudokuViewModel: ViewModel created
D/SudokuViewModel: Selected cell changed: index=2
D/SudokuViewModel: Validation completed: conflicts=[]
D/SudokuViewModel: Cell value changed: index=2, from=0, to=4
I/SudokuViewModel: Starting Sudoku solver: clues=31
D/SudokuViewModel: Validation completed: conflicts=[]
I/SudokuViewModel: Mode changed: EDIT -> SOLUTION
I/SudokuViewModel: Sudoku solved successfully
```

The board still logs clicks under `SudokuBoard`, and the Activity still logs its
lifecycle under `MainActivity`. Rotating normally recreates the Activity but
should not print another `ViewModel created` message. Validation now runs from
board-changing actions, not from a screen effect after recreation. The known
valid initial/reset puzzle uses an empty conflict set directly.

- If `viewModel` is unresolved, check its import, dependency alias, and Gradle sync.
- If state resets during ordinary rotation, verify the screen uses the framework
  `viewModel()` function and has no local duplicate state.
- If digits or conflict colors stop updating, assign a new `uiState` and copied
  arrays rather than mutating arrays exposed to the UI.
- If Preview complains about a ViewModel owner, preview `SudokuScreenContent`
  with sample state instead of calling `SudokuScreen`.

**Persistence boundary:** Step 06C used `rememberSaveable`; this refactor uses
an in-memory ViewModel. It preserves state across normal configuration changes,
but this implementation does not restore the puzzle after process death or a
fresh launch. That is a narrower guarantee than saved-state restoration. Do not
use force-stop or “Don't keep activities” as a rotation test. Android's
[saving UI state guide](https://developer.android.com/develop/ui/compose/state-saving)
explains the different lifetimes and `SavedStateHandle` support.

## Run / test

In Android Studio, sync Gradle, build, and run on the course's Android 15 / API 35
device. Start in portrait with normal Activity lifecycle settings.

1. Check that the initial board and controls look unchanged. Press a digit and
   Clear without a selection; verify the warning logs and unchanged board.
2. Select row 0, column 2 (index 2) and enter **4**. Rotate to landscape and back.
   Verify the edited digit and selected highlight survive. Check Activity
   recreation logs and that `ViewModel created` was not repeated.
3. Clear that cell, then enter **7**. Verify the row conflict highlights at
   indices 2 and 4. Rotate and return: selection and conflicts must survive.
   Solve should log rejection and remain in Edit mode.
4. Reset, enter **1** at index 2, and tap Solve. This puzzle has no immediate
   duplicates but no solution. Verify the board remains editable and unchanged.
5. Reset, enter **4** at index 2, and solve. Verify the solution, clue styling,
   hidden number pad, and disabled cell clicks.
6. Rotate and return in Solution mode. Verify the solution and styling survive.
   Edit clues must restore your 4 and remove generated digits.
7. Verify Reset restores the original 30 clues, clears selection/conflicts, and
   shows editing controls from both modes.
8. Open `SudokuScreenPreview` and check the initial puzzle renders without a
   ViewModel owner or runtime error.

Landscape may still crowd or clip controls because the board uses width-based
square sizing. Return to portrait to operate the controls; this step checks
state retention, not an adaptive layout.

Run the existing local JVM `SudokuValidatorTest` and `SudokuSolverTest` classes
as regression checks. They exercise the unchanged domain logic, not the new
ViewModel or Activity lifecycle. Use the manual checks above for this refactor.

## Expected result

- ✓ The ViewModel owns all five state fields and all six user actions.
- ✓ Compose renders state and forwards events through callbacks.
- ✓ Editing, validation, solving, clue restoration, and Reset still work.
- ✓ Normal rotation retains the current state and ViewModel instance.
- ✓ The preview renders without constructing a ViewModel.

## Architecture after this step

```text
MainActivity
  └── SudokuScreen (obtains the Activity-scoped ViewModel)
        └── SudokuScreenContent
              ├── SudokuBoard → SudokuCell
              └── NumberPad and action buttons

SudokuScreenContent -- events --> SudokuViewModel
SudokuScreenContent <-- uiState -- SudokuViewModel
                                    ├── SudokuValidator
                                    └── SudokuSolver
```

`ui/` now contains `SudokuMode`, `SudokuUiState`, and `SudokuViewModel` alongside
the existing theme package. The composables keep their existing package, and
the domain layer continues to work with `IntArray` values.
