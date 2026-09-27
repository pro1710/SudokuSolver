# Step 07C — Migrate the app to the Sudoku domain model

## Goal

Use the tested `SudokuBoard` model throughout validation, solving, ViewModel
state, and the grid. Solving returns a new board or `null` without modifying its
input. The app's visible behavior remains the same.

## Starting point

Start from Step 07B, commit `9086789` (`step-07b: introduce sudoku domain model`).
`CellPosition` and the domain `SudokuBoard` are implemented and tested. The app
still uses `IntArray`, and `SudokuSolver.solve` returns a Boolean while mutating
the caller's working array. We now migrate those consumers in dependency order.

## Concepts introduced

- An **API contract** describes what callers pass, receive, and can expect to
  remain unchanged. The new solver contract promises to preserve its input.
- An **immutable public API** can use a **mutable private implementation**. A
  private copied array lets backtracking try and undo candidates without exposing
  those mutations to the rest of the application.
- A **nullable result**, `SudokuBoard?`, represents either a solved board or no
  solution. Checking a local result with `!= null` lets Kotlin treat it as a
  non-null board inside that branch.
- Immutable objects can be shared safely. The displayed board and accepted
  clues may reference the same object until an edit or solve returns another one.

Review Kotlin's [null-safety guide](https://kotlinlang.org/docs/null-safety.html)
for nullable types and smart casts. Android's
[Compose state guide](https://developer.android.com/develop/ui/compose/state)
explains why assigning observable state, rather than mutating hidden data,
drives UI updates.

## Implementation

### 1. Let the validator read the model

In `domain/SudokuValidator.kt`, change the public parameter:

```kotlin
fun findConflicts(board: SudokuBoard): Set<Int>
```

Change the private `findConflictsInGroup` parameter to `SudokuBoard` too.
Everything else in the validator stays the same: the row/column/box traversal,
zero handling, and set of conflicting indices. Expressions such as `board[index]`
now call the model's `operator get` instead of indexing an array directly.
There is no need to export an array just to read the board.

Update `SudokuValidatorTest` fixtures. Wrap the classic puzzle in
`SudokuBoard(intArrayOf(...))`. For focused conflict tests, use immutable updates:

```kotlin
val board = SudokuBoard()
    .withValue(18, 5)
    .withValue(26, 5)

val conflicts = SudokuValidator.findConflicts(board)
assertEquals(setOf(18, 26), conflicts)
```

Keep all eight existing cases and expected conflict sets. For the unchanged-input
assertion, capture `board.toIntArray()` before validation and compare it with a
fresh export afterward. In the empty-cell test, store the result of `withValue`
in a new variable instead of assigning into the board.

### 2. Change only the solver's public boundary

Replace the public method in `domain/SudokuSolver.kt` with:

```kotlin
fun solve(board: SudokuBoard): SudokuBoard? {
    if (SudokuValidator.findConflicts(board).isNotEmpty()) return null
    val workingBoard = board.toIntArray()
    return if (solveNextCell(workingBoard)) SudokuBoard(workingBoard) else null
}
```

The input model guarantees 81 values in `0..9`; the validator checks Sudoku
conflicts. `toIntArray()` returns a defensive copy. The existing recursion may
mutate this copy, but it cannot reach the original model's private array.

Keep `solveNextCell(board: IntArray): Boolean` and `canPlaceNumber` unchanged.
They still choose an empty cell, try candidates, recurse, and clear unsuccessful
candidates. On success, the public method wraps the filled working array in a
new model; otherwise it returns `null`. A conflicting filled board is rejected
before the recursion can mistake it for a solution.

```text
Caller supplies SudokuBoard (unchanged)
                  │ toIntArray() copies
                  ▼
           private IntArray
           try / recurse / undo
                  │
                  ├── success → new SudokuBoard
                  └── failure → null
```

Both conflicting inputs and puzzles with no possible solution return `null`.
The UI still uses its conflict set to log the more specific rejection first.

### 3. Test the new contract

In `SudokuSolverTest`, keep the puzzle and exact solution arrays as fixtures,
but wrap inputs in `SudokuBoard`. Use `withValue` to construct the conflicting
and unsolvable cases.

Replace success Boolean assertions with `assertNotNull(solved)` and compare
`solved?.toIntArray()` against the expected digits. The safe-call operator `?.`
allows the nullable result to reach the array assertion; `assertNotNull` gives
a clear failure before that comparison if solving fails. Import `assertNull`
and `assertNotNull` from `org.junit.Assert`.

Replace failure assertions with `assertNull(solved)`. Continue comparing input
contents before and after each rejected solve. Rename the unsolvable test to
describe its public contract: `unsolvable board returns null without changing
the input`. Undoing candidates remains a private algorithm detail.

Add an explicit successful-solve immutability test:

```kotlin
@Test
fun `successful solving does not modify the input board`() {
    val board = SudokuBoard(examplePuzzle())
    val originalValues = board.toIntArray()

    val solved = SudokuSolver.solve(board)

    assertNotNull(solved)
    assertArrayEquals(originalValues, board.toIntArray())
    assertArrayEquals(expectedSolution(), solved?.toIntArray())
}
```

The first array assertion protects the input; the second ensures a real solution
was produced. Keep the known-solution, already-solved, conflicting-clue,
unsolvable, and invalid-filled-board tests. The solver now has six tests.

These remain local JVM tests. Android's
[local unit test guide](https://developer.android.com/training/testing/local-tests)
describes their placement in `src/test` and running logic tests without an emulator.

### 4. Store models in UI state

Import `com.example.sudokusolver.domain.SudokuBoard` in `ui/SudokuUiState.kt`.
Change the first two properties:

```kotlin
val board: SudokuBoard = createInitialBoard(),
val originalBoard: SudokuBoard = board,
```

Leave selection, conflicts, and mode unchanged. Change `createInitialBoard()`
to return `SudokuBoard`, wrapping its existing `intArrayOf(...)` in the model
constructor. Keep all 81 puzzle values unchanged.

`originalBoard = board` is now safe: neither reference allows in-place editing.
An immutable update creates another object, so sharing the initial object cannot
accidentally change accepted clues. The regular board class still uses reference
equality; no new equality implementation is needed for this migration.

### 5. Simplify the ViewModel's array handling

Import the domain `SudokuBoard` in `ui/SudokuViewModel.kt`. In
`updateSelectedCell`, replace copied-array edits with:

```kotlin
val newBoard = uiState.board.withValue(index, number)
val newOriginalBoard = uiState.originalBoard.withValue(index, number)
uiState = uiState.copy(
    board = newBoard,
    originalBoard = newOriginalBoard,
    conflicts = findConflicts(newBoard)
)
```

Keep the existing mode guard, missing-selection warning, and change/clear logs.
Change the private validation helper's parameter to `SudokuBoard`; its logging
and call to the validator stay the same. Board changes and their conflict set
are still published in one state assignment.

In `solve()`, retain the Edit-mode guard and conflict rejection. Replace the
manual working-array copy and clue counting with:

```kotlin
val clueCount = uiState.originalBoard.clueCount
Log.i("SudokuViewModel", "Starting Sudoku solver: clues=$clueCount")
val solvedBoard = SudokuSolver.solve(uiState.board)
if (solvedBoard != null) {
    uiState = uiState.copy(
        board = solvedBoard,
        selectedCell = null,
        conflicts = findConflicts(solvedBoard),
        mode = SudokuMode.SOLUTION
    )
    Log.i("SudokuViewModel", "Mode changed: EDIT -> SOLUTION")
    Log.i("SudokuViewModel", "Sudoku solved successfully")
} else {
    Log.w("SudokuViewModel", "Sudoku has no solution")
}
```

The solver now owns its working copy. On failure, the ViewModel leaves its state
unchanged. On success, it changes the displayed board but preserves
`originalBoard`, maintaining clue-versus-generated-digit styling.

In `editClues()`, use `val clueBoard = uiState.originalBoard` instead of copying
an array. Keep the following state assignment and mode log. Reset continues to
assign `SudokuUiState()`. Selection and the six public action names stay the same.

The ViewModel still assigns a new `uiState` through `mutableStateOf`, which is
what notifies Compose. Immutability protects old board contents; it does not
replace observable state. Solving remains synchronous as in the previous step.

### 6. Rename the visual board to SudokuGrid

Rename the root-package UI file `SudokuBoard.kt` to `SudokuGrid.kt` and the
composable to `SudokuGrid`. This distinguishes the UI component from the domain
class now used in its parameters.

Import `com.example.sudokusolver.domain.SudokuBoard` and change both `board`
and `originalBoard` parameter types to `SudokuBoard`. Existing reads such as
`board[index]` and `originalBoard[index] != 0` continue working through the
model's lookup operator. Preserve the layout, Canvas, selection, and error styling.

Update the call in `SudokuScreenContent` from `SudokuBoard(...)` to
`SudokuGrid(...)`. Rename the preview to `SudokuGridPreview`, update its call,
and replace the preview's mutable-array setup with:

```kotlin
val originalBoard = SudokuBoard().withValue(0, 5).withValue(8, 5)
val board = originalBoard.withValue(4, 7)
```

The preview still illustrates two conflicting clues and an accent-colored
generated digit. Change the cell-click log tag to `SudokuGrid` to match the
component's new name. Keep the composables in their current package for this step.

## Logging / troubleshooting

Filter `package:com.example.sudokusolver tag:SudokuGrid` for cell clicks, or
`tag:SudokuViewModel` for actions and validation. Expected examples include:

```text
D/SudokuGrid: Cell clicked: index=2, row=0, column=2, value=0
D/SudokuViewModel: Validation completed: conflicts=[]
D/SudokuViewModel: Cell value changed: index=2, from=0, to=4
I/SudokuViewModel: Starting Sudoku solver: clues=31
D/SudokuViewModel: Validation completed: conflicts=[]
I/SudokuViewModel: Mode changed: EDIT -> SOLUTION
I/SudokuViewModel: Sudoku solved successfully
```

The validator and solver still have no Android logging or per-recursion logs.

- If an old caller reports an `IntArray`/`SudokuBoard` type mismatch, migrate the
  caller to the model. Keep arrays inside fixtures, initial construction, and the
  private backtracking implementation.
- If code treats `solve` as Boolean, replace that check with a nullable-result
  check and use the returned board.
- If `SudokuBoard(...)` appears where the UI should render, check the component
  rename and import. The model constructor does not draw anything.
- If Edit clues keeps generated digits, ensure solving never assigns its result
  to `originalBoard`.
- If an edit is ignored, retain the result of `withValue` in a new `uiState`.

## Run / test

1. In Android Studio, run `CellPositionTest`, `SudokuBoardTest`,
   `SudokuValidatorTest`, and `SudokuSolverTest`. Expect **34 domain tests** to
   pass: 6 coordinate, 14 board, 8 validator, and 6 solver tests.
2. Build the app and open `SudokuGridPreview` and `SudokuScreenPreview`. Verify
   that the component rename and new parameter types compile and render.
3. Run on the course's Android 15 / API 35 device. Select index 2 (row 0,
   column 2), enter **4**, and solve. Check that this clue stays bold while
   generated digits use the accent color. Solution mode must disable editing.
4. Tap Edit clues. Verify the 4 remains and generated digits disappear. Clear
   the 4, then Reset and confirm the built-in 30 clues return.
5. Test conflicts separately, resetting between cases: enter **7** at index 2
   for a row conflict, **8** at index 3 for a column conflict, and **8** at index
   10 for a box conflict. Verify error colors and Solve rejection each time.
6. Reset, enter **1** at index 2, then Solve. This has no immediate conflict but
   no solution. The entered clues should stay unchanged in Edit mode, with the
   existing no-solution log.
7. Reset, make a valid edit, and rotate to landscape and back. Repeat after
   solving. Confirm selection, clues, conflict state, and mode survive normal
   rotation. Landscape layout retains its existing width-based sizing limitation.

## Expected result

- ✓ Validator and solver callers use `SudokuBoard`.
- ✓ Successful solving returns the expected solution without modifying its input.
- ✓ Rejected or unsolvable boards return `null` and remain unchanged.
- ✓ UI state contains immutable board objects; edits use `withValue`.
- ✓ The renamed grid preserves all existing visuals and interactions.
- ✓ All 34 domain tests pass.

## Architecture after this step

```text
SudokuScreenContent
  ├── SudokuGrid → SudokuCell
  └── NumberPad and action buttons
           │ events
           ▼
     SudokuViewModel
       ├── SudokuUiState (SudokuBoard objects)
       ├── SudokuValidator.findConflicts(SudokuBoard)
       └── SudokuSolver.solve(SudokuBoard): SudokuBoard?
                  └── private copied IntArray for backtracking
```

The tested board model is now used end to end. The public boundary preserves
board values while the solver's private recursion retains its straightforward
mutable working representation.
