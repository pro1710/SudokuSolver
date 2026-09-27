# Step 05B — Full Sudoku validation

## Goal

Extend validation to rows, columns, and 3×3 boxes. The public function becomes
`SudokuValidator.findConflicts(board)` and returns all conflicting cell indices
as a `Set<Int>`. Keep the validator pure Kotlin and test it independently of
the UI.

## Starting point

Start from Step 05A, commit `ddb6ae9`
(`step-05a: add row validation with unit tests`). The domain package contains
`SudokuValidator.findRowConflicts(IntArray)`, which skips zeros and compares
pairs within a row. Its five JVM tests use readable names in backticks.

The screen still allows editing without displaying conflicts. This step
changes only the validator, its tests, and documentation.

## Concepts introduced

- **Refactoring** changes how code is organized while preserving existing
  behavior. First extract pair comparison into one helper; then reuse it to
  add the new rules.
- **A group of indices** represents a row, column, or box. All three require
  the same duplicate check, so only the calculation of their indices differs.
- **`List<Int>`** stores the group's board indices in order. A position in
  this list is different from an index into the 81-cell board.
- **`map`** transforms each element of a range into another value. We use it
  to turn column or row numbers into board indices.
- **`private`** limits the helper to the validator object. Callers and tests
  use the public `findConflicts` function.
- **Set union with `addAll`** combines each group's conflicts. A cell that
  conflicts in more than one group appears only once in the final set.

Test the observable rules rather than calling the private helper directly.
This lets the implementation evolve without tying tests to its internal
structure. Android's [What to test](https://developer.android.com/training/testing/fundamentals/what-to-test)
guide discusses testing behavior and keeping implementation details out of
tests.

## Implementation

### 1. Extract the existing pair comparison

Inside `SudokuValidator`, add this private function below the current public
function:

```kotlin
private fun findConflictsInGroup(board: IntArray, indices: List<Int>): Set<Int> {
    val conflicts = mutableSetOf<Int>()

    for (position in indices.indices) {
        val index = indices[position]
        val value = board[index]
        if (value == 0) continue

        for (otherPosition in position + 1 until indices.size) {
            val otherIndex = indices[otherPosition]
            if (board[otherIndex] == value) {
                conflicts.add(index)
                conflicts.add(otherIndex)
            }
        }
    }

    return conflicts
}
```

This keeps the pair-comparison algorithm from Step 05A, but replaces direct
row/column calculations with lookups in `indices`. `indices.indices` is the
range of valid **list positions**. `indices[position]` retrieves an actual
**board index**, and `board[index]` retrieves the digit.

For example, column 8 is represented by:

```text
list position: 0   1   2   3   4   5   6   7   8
board index:   8  17  26  35  44  53  62  71  80
```

The helper returns board indices, not list positions. It still skips zero,
compares only later positions, and adds both cells of each matching pair.

### 2. Reuse the helper for rows first

In `findRowConflicts`, retain the result set and final `return conflicts`.
Replace its nested comparison loops with:

```kotlin
for (row in 0 until 9) {
    val indices = (0 until 9).map { column -> row * 9 + column }
    conflicts.addAll(findConflictsInGroup(board, indices))
}
```

For row 2, `map` produces `[18, 19, 20, 21, 22, 23, 24, 25, 26]`.
The rule has not expanded yet: at this intermediate point the original five
Step 05A tests should still pass. This is a useful checkpoint when reproducing
the refactor manually; it is not a separate educational commit.

### 3. Rename the public function and add columns

Rename `findRowConflicts` to `findConflicts`. Keep the parameter and return
types: `board: IntArray` and `Set<Int>`. Update all calls in
`SudokuValidatorTest` to the new name. Earlier tutorials describe their own
historical commits, so their old API examples stay unchanged.

After the row loop and before the return, add:

```kotlin
for (column in 0 until 9) {
    val indices = (0 until 9).map { row -> row * 9 + column }
    conflicts.addAll(findConflictsInGroup(board, indices))
}
```

The column stays fixed while the row varies. Each index is nine positions
after the previous one. The same helper now detects vertical duplicates.

### 4. Add the nine boxes

After the column loop, add:

```kotlin
for (boxRow in 0 until 3) {
    for (boxColumn in 0 until 3) {
        val indices = mutableListOf<Int>()
        for (rowOffset in 0 until 3) {
            for (columnOffset in 0 until 3) {
                val row = boxRow * 3 + rowOffset
                val column = boxColumn * 3 + columnOffset
                indices.add(row * 9 + column)
            }
        }
        conflicts.addAll(findConflictsInGroup(board, indices))
    }
}
```

`boxRow` and `boxColumn` identify a box in the 3×3 arrangement of boxes.
Multiplying by three locates its top-left cell. Offsets `0`, `1`, and `2`
then visit its nine cells.

For the bottom-right box, `boxRow = 2` and `boxColumn = 2`, so its cells are:

```text
60 61 62
69 70 71
78 79 80
```

Build a fresh list inside each box iteration. Add its conflicts to the one
result set declared at the beginning of `findConflicts`. The method now checks
27 groups: nine rows, nine columns, and nine boxes. It never changes the input
array. The input contract remains 81 entries with values in `0..9`.

### 5. Update fixtures that were valid only for rows

Two earlier tests intentionally accepted boards that violate the new rules:

- Rename `valid rows have no conflicts` to `valid board has no conflicts`.
  Replace its repeated `1..9` row initializer with the complete 81-value
  `intArrayOf(...)` puzzle from `SudokuScreen`. Copy the fixture's values into
  the test; do not call or import the composable. The supplied puzzle contains
  no row, column, or box conflicts.
- In `empty cells are ignored`, keep the empty-board assertion, then set
  `board[0] = 5` and `board[1] = 3`. Replace the former `board[9] = 5`, which
  would now conflict with index 0 in both the first column and first box.

Keep `duplicate values in a row are conflicts` and its input-preservation
assertion. Indices 18 and 26 share a row but neither a column nor a box, so
this remains an isolated row test. Also retain
`all occurrences of a repeated value are conflicts` with indices 72, 75, and
80; it confirms that every matching cell is returned.

### 6. Add isolated column and box tests

Add to the existing `SudokuValidatorTest` class:

```kotlin
@Test
fun `duplicate values in a column are conflicts`() {
    val board = IntArray(81)
    board[8] = 6
    board[80] = 6

    val conflicts = SudokuValidator.findConflicts(board)

    assertEquals(setOf(8, 80), conflicts)
}
```

These cells share the last column but occupy different rows and boxes.

```kotlin
@Test
fun `duplicate values in a box are conflicts`() {
    val board = IntArray(81)
    board[60] = 8
    board[80] = 8

    val conflicts = SudokuValidator.findConflicts(board)

    assertEquals(setOf(60, 80), conflicts)
}
```

These cells share the bottom-right box but neither a row nor a column. This
test would fail if box validation were missing or checked only the first box.

### 7. Allow matching digits in unrelated cells

```kotlin
@Test
fun `same value in unrelated cells is allowed`() {
    val board = IntArray(81)
    board[0] = 5
    board[40] = 5

    val conflicts = SudokuValidator.findConflicts(board)

    assertEquals(emptySet<Int>(), conflicts)
}
```

Index 0 is at row 0, column 0; index 40 is at row 4, column 4. They also belong
to different boxes. Sudoku prohibits duplicates within a group, not across
the entire board.

### 8. Collect conflicts from all three kinds of group

Replace `conflicts from different rows are collected` with:

```kotlin
@Test
fun `conflicts from rows columns and boxes are collected`() {
    val board = IntArray(81)
    board[0] = 4
    board[8] = 4
    board[9] = 2
    board[63] = 2
    board[60] = 7
    board[70] = 7
    board[40] = 9

    val conflicts = SudokuValidator.findConflicts(board)

    assertEquals(setOf(0, 8, 9, 63, 60, 70), conflicts)
}
```

The `4`s conflict in a row, the `2`s in a column, and the `7`s in a box.
The single `9` is unrelated and must not be included. Exact set equality
checks both that all expected conflicts are present and that no extra cell
was reported.

## Logging / troubleshooting

The validator still has no Android imports or logging. Observe results in
JUnit assertions. UI logs and behavior remain unchanged because the screen
does not call validation yet.

- **Old tests fail after adding columns:** update the row-only fixtures from
  section 5. Their previous expectations described a narrower contract.
- **Unresolved `findRowConflicts`:** replace old calls in the current test
  source with `findConflicts`.
- **Returned indices are always between 0 and 8:** add `indices[position]`
  and `indices[otherPosition]` to the result, not the positions themselves.
- **Box duplicates are missed:** calculate each box origin with `boxRow * 3`
  and `boxColumn * 3`, then add the cell offsets.
- **Unrelated equal digits conflict:** ensure every helper call receives
  exactly one row, column, or box, rather than all 81 cells.
- **Only the last group's result remains:** use `addAll` on the shared result
  set and return it only after all loops finish.

## Run / test

In Android Studio, run the entire `SudokuValidatorTest` class under
`app/src/test/java`. It should now report **eight passing tests**:

| Test | Purpose |
|---|---|
| `valid board has no conflicts` | Accept the supplied puzzle |
| `duplicate values in a row are conflicts` | Isolate row validation and preserve input |
| `duplicate values in a column are conflicts` | Isolate column validation |
| `duplicate values in a box are conflicts` | Isolate box validation |
| `empty cells are ignored` | Accept empty and partially filled boards |
| `same value in unrelated cells is allowed` | Avoid treating all matching digits as conflicts |
| `all occurrences of a repeated value are conflicts` | Report all three matching cells |
| `conflicts from rows columns and boxes are collected` | Combine all conflict types |

These remain ordinary JVM tests using the existing JUnit dependency. No
emulator, new library, or Android mocking is needed. See the official
[local unit test guide](https://developer.android.com/training/testing/local-tests)
for the source-set and test-runner setup.

Build the app in Android Studio as well. If you run it, expect the same
editable board as Step 04; visible conflict feedback comes in Step 05C.

## Expected result

- ✓ `findConflicts(IntArray)` checks rows, columns, and all nine boxes.
- ✓ Every participating cell is returned once; zeros are ignored.
- ✓ Equal digits in unrelated cells remain allowed.
- ✓ The input array is unchanged and eight JVM tests pass.
- ✓ The UI remains independently editable without validation feedback.

## Architecture after this step

```text
SudokuValidator.findConflicts(board)
    ├── row indices ─────┐
    ├── column indices ─┼── findConflictsInGroup(board, indices)
    └── box indices ────┘               │
                                        ▼
                               combined Set<Int>
```

One helper defines the duplicate rule. The public method defines which cells
belong together and combines their results. Tests exercise the public method;
Compose integration remains a separate step.
