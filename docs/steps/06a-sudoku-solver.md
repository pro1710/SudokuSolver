# Step 06A — Sudoku backtracking solver

## Goal

Implement a pure Kotlin solver that fills a working array using recursive
backtracking. At this historical step its public API is:

```kotlin
fun solve(board: IntArray): Boolean
```

It returns `true` with the solution written into the supplied array, or `false`
if no solution is found. Failed searches undo their attempted values. The
Compose UI does not call the solver yet.

## Starting point

Start from Step 05C, commit `0496b34`
(`step-05c: highlight sudoku conflicts in ui`). The app supports editing and
conflict highlighting. `SudokuValidator.findConflicts(IntArray)` is already
tested for rows, columns, and boxes.

Add `domain/SudokuSolver.kt` and a corresponding local JVM test class. Keep
the validator, UI, dependencies, and existing tests unchanged.

## Concepts introduced

- **Recursion** is a function calling itself on a smaller remaining problem.
  Here, each successful placement leaves one fewer empty cell to fill.
- **A base case** ends recursion. When there are no empty cells, the current
  board is a solution because initial clues were checked and every added
  number respected the Sudoku rules.
- **Candidate search** tries digits `1..9` in an empty cell and rejects those
  already present in its row, column, or box.
- **Backtracking** means undoing a choice after discovering that it cannot
  lead to a solution, then trying the next candidate.
- **Shared mutable working data** lets recursive calls work on the same
  array. Each call must undo its own failed placement so it does not affect
  a later attempt.

Validation and solving answer different questions. Validation finds repeated
digits already on the board. Solving determines whether all remaining cells
can be filled consistently. No current conflicts does not guarantee a solution.

## Implementation

### 1. Define the solver's entry point

Create `app/src/main/java/com/example/sudokusolver/domain/SudokuSolver.kt`:

```kotlin
package com.example.sudokusolver.domain

object SudokuSolver {
    fun solve(board: IntArray): Boolean {
        if (SudokuValidator.findConflicts(board).isNotEmpty()) return false
        return solveNextCell(board)
    }
}
```

The private `solveNextCell` function will be added next. Validate the starting
clues once at the entry point. Otherwise a fully filled but invalid board
could incorrectly pass the no-empty-cells base case. Recursive calls will
check only their candidate placements.

As with the validator, callers supply exactly 81 integers in `0..9`. Zero is
empty. This API intentionally mutates its argument on success; it does not
return a separate solution array. It finds the first solution rather than
counting solutions or proving uniqueness.

### 2. Find the next empty cell and the base case

Inside the object, add the private function below. Begin by finding an empty
cell and converting its index back to coordinates:

```kotlin
private fun solveNextCell(board: IntArray): Boolean {
    val index = board.indexOfFirst { value -> value == 0 }
    if (index == -1) return true

    val row = index / 9
    val column = index % 9

    // Add the candidate loop here in the next edit.
    return false
}
```

`indexOfFirst` returns `-1` when no entry matches. Check for that value before
using it as an index. Integer division `/ 9` gives the row and remainder `% 9`
gives the column. For index 23, these are row 2 and column 5.

### 3. Try candidates, recurse, and undo failures

Replace the placeholder comment with this loop, retaining the final
`return false`:

```kotlin
for (number in 1..9) {
    if (canPlaceNumber(board, row, column, number)) {
        board[index] = number
        if (solveNextCell(board)) return true
        board[index] = 0
    }
}
```

The next edit supplies `canPlaceNumber`. After a permitted placement, the
recursive call tries to finish the rest of the board. A successful result
returns immediately, keeping the solved values in the array.

If the deeper search fails, reset this cell to zero before trying another
number. Deeper calls have already undone their own failed placements. If all
nine candidates fail, return `false` to let the caller undo its earlier choice.

```text
choose first empty cell
    ↓
try a permitted digit
    ↓
solve remaining cells recursively
    ├── success → keep values and return true
    └── failure → restore zero and try next digit
                         ↓
                no candidates left → return false
```

Every recursive call advances after filling one zero, so recursion cannot
keep choosing the same empty cell indefinitely. Existing clues are never
selected for replacement. This simple search can still explore many branches
on difficult puzzles; the step focuses on a readable algorithm.

### 4. Check the row, column, and box before placing a digit

Add this private helper after `solveNextCell`:

```kotlin
private fun canPlaceNumber(board: IntArray, row: Int, column: Int, number: Int): Boolean {
    for (offset in 0 until 9) {
        if (board[row * 9 + offset] == number) return false
        if (board[offset * 9 + column] == number) return false
    }

    val boxStartRow = row / 3 * 3
    val boxStartColumn = column / 3 * 3
    for (boxRow in boxStartRow until boxStartRow + 3) {
        for (boxColumn in boxStartColumn until boxStartColumn + 3) {
            if (board[boxRow * 9 + boxColumn] == number) return false
        }
    }

    return true
}
```

The first two checks hold the row or column fixed while varying the other
coordinate. `row / 3 * 3` finds the first row of the cell's box: rows 0–2 map
to 0, rows 3–5 to 3, and rows 6–8 to 6. The column calculation works similarly.

The target cell is still zero when this helper runs, so it cannot match a
candidate from `1..9`. The helper only reads the array. It returns `true` when
none of the three groups already contains the candidate.

### 5. Set up fresh test fixtures

Create `app/src/test/java/com/example/sudokusolver/domain/SudokuSolverTest.kt`
with package `com.example.sudokusolver.domain`, a `SudokuSolverTest` class, and
these imports:

```kotlin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
```

Inside the class, add `private fun examplePuzzle(): IntArray = intArrayOf(...)`
using all 81 values of the built-in puzzle already shown in
`SudokuValidatorTest` and `SudokuScreen`. Copy the data into the test rather
than depending on Compose. The `=` syntax returns the array expression from
the function. Each call creates a fresh array, so one test's mutations cannot
leak into another.

Also add this fixture with the known completed values:

```kotlin
private fun expectedSolution(): IntArray = intArrayOf(
    5, 3, 4, 6, 7, 8, 9, 1, 2,
    6, 7, 2, 1, 9, 5, 3, 4, 8,
    1, 9, 8, 3, 4, 2, 5, 6, 7,
    8, 5, 9, 7, 6, 1, 4, 2, 3,
    4, 2, 6, 8, 5, 3, 7, 9, 1,
    7, 1, 3, 9, 2, 4, 8, 5, 6,
    9, 6, 1, 5, 3, 7, 2, 8, 4,
    2, 8, 7, 4, 1, 9, 6, 3, 5,
    3, 4, 5, 2, 8, 6, 1, 7, 9
)
```

These are JVM tests under `src/test`, using the existing JUnit 4 dependency.
Android's [local unit test guide](https://developer.android.com/training/testing/local-tests)
explains this source set and the assertion-based test setup.

### 6. Assert the actual solution, not just success

Add this test using the course's backtick naming style:

```kotlin
@Test
fun `known puzzle is solved with the expected values`() {
    val board = examplePuzzle()

    val solved = SudokuSolver.solve(board)

    assertTrue(solved)
    assertArrayEquals(expectedSolution(), board)
}
```

Returning `true` alone would not prove that the solver filled the cells
correctly. Exact array comparison checks all 81 values, including preservation
of the original clues. Test the public behavior rather than the number or
order of recursive calls, following the approach discussed in Android's
[What to test](https://developer.android.com/training/testing/fundamentals/what-to-test)
guide.

### 7. Test the solved-board base case and conflicting input

```kotlin
@Test
fun `already solved board is accepted unchanged`() {
    val board = expectedSolution()
    val originalBoard = board.copyOf()

    val solved = SudokuSolver.solve(board)

    assertTrue(solved)
    assertArrayEquals(originalBoard, board)
}
```

```kotlin
@Test
fun `conflicting clues are rejected without changing the board`() {
    val board = examplePuzzle()
    board[2] = 5
    val originalBoard = board.copyOf()

    val solved = SudokuSolver.solve(board)

    assertFalse(solved)
    assertArrayEquals(originalBoard, board)
}
```

The extra `5` conflicts with an existing clue. Add a separate case for a
filled but invalid board, which must not be mistaken for success:

```kotlin
@Test
fun `completed board with conflicts is rejected`() {
    val board = expectedSolution()
    board[0] = 3
    val originalBoard = board.copyOf()

    val solved = SudokuSolver.solve(board)

    assertFalse(solved)
    assertArrayEquals(originalBoard, board)
}
```

### 8. Test an unsolvable board and rollback

```kotlin
@Test
fun `unsolvable board restores all attempted values`() {
    val board = examplePuzzle()
    board[2] = 1
    val originalBoard = board.copyOf()
    assertTrue(SudokuValidator.findConflicts(board).isEmpty())

    val solved = SudokuSolver.solve(board)

    assertFalse(solved)
    assertArrayEquals(originalBoard, board)
}
```

Putting `1` at index 2 introduces no immediate duplicate, but the puzzle with
that added clue has no solution. The first assertion ensures this case reaches
the search rather than being rejected by initial conflict checking. The final
assertion confirms that all attempted placements were undone, preserving the
input including the extra clue. This is different from the previous invalid
input test: the failure is discovered through search.

## Logging / troubleshooting

There is no Android logging inside the solver and no new Logcat output.
Logging each candidate would produce excessive output. Use the local test
results or Android Studio debugger to inspect a failing search instead.

- **The solver returns true but values remain empty:** check the base case
  and the recursive success path; compare the whole result array in tests.
- **Valid puzzles fail or incorrect candidates remain:** restore
  `board[index] = 0` after each failed recursive call.
- **The solution disappears while returning:** do not clear a candidate when
  its recursive call succeeded. Return `true` first.
- **A filled invalid board is accepted:** keep the validator check in the
  public entry point before entering the recursive helper.
- **The unsolvable test changes its input:** a recursive failure path left an
  attempted digit behind. Each call must restore its own selected cell.
- **Array assertions fail unexpectedly:** use `assertArrayEquals` for
  contents; ordinary array equality does not compare all element values.
- **Tests interfere with each other:** create a new puzzle array for each
  test rather than sharing one mutable fixture.

## Run / test

In Android Studio, run `SudokuSolverTest` under `app/src/test/java`.
All five tests should pass:

| Test | What it verifies |
|---|---|
| `known puzzle is solved with the expected values` | All 81 solution values match |
| `already solved board is accepted unchanged` | The valid completed-board base case |
| `conflicting clues are rejected without changing the board` | Invalid starting clues are rejected |
| `unsolvable board restores all attempted values` | Search failure undoes temporary placements |
| `completed board with conflicts is rejected` | A full board must still obey Sudoku rules |

Run the existing `SudokuValidatorTest` as well: the two domain test classes
now contain **13 tests** in total. These tests run on the workstation JVM and
do not require an emulator.

Build the app in Android Studio. If you run it, editing and conflict feedback
should behave exactly as in Step 05C. There is no Solve button yet; connecting
this solver to the screen is the separate Step 06B.

## Expected result

- ✓ `SudokuSolver.solve(IntArray)` returns a Boolean and fills its argument on success.
- ✓ Existing clues remain unchanged, and the known puzzle matches its exact solution.
- ✓ Conflicting input and an unsolvable puzzle return false.
- ✓ Failed attempts are undone, leaving the unsuccessful input board unchanged.
- ✓ All 13 domain tests pass while the app UI retains its previous behavior.

## Architecture after this step

```text
Local JVM tests → SudokuSolver.solve(workingArray)
                     ├── SudokuValidator checks starting clues
                     └── solveNextCell
                           ├── canPlaceNumber
                           ├── place candidate
                           ├── recursive solveNextCell
                           └── undo candidate on failure

Compose UI → SudokuValidator (existing conflict feedback)
```

The solver works only with Kotlin data and stays separate from Compose.
Its mutable-array API is intentional at this point in the course; later steps
will teach how to expose an immutable domain API.
