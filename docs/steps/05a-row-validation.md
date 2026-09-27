# Step 05A — Row validation

## Goal

Write and test a Kotlin function that finds repeated nonzero digits in each
row. It returns the indices of all cells involved in row conflicts.
The editable app remains visually unchanged: this step tests the rule in
isolation, before connecting validation to Compose.

## Starting point

Start from Step 04, commit `bb1fe2a`
(`step-04: add editable board and number pad`). The screen owns an `IntArray`
with 81 values and supports selection, number entry, and Clear.

The project already includes JUnit 4 through `testImplementation(libs.junit)`
in `app/build.gradle.kts`. Reuse it without adding dependencies or changing
versions. Keep the generated example tests.

## Concepts introduced

- **Domain logic** describes the problem being solved. Whether a Sudoku row
  contains a repeated digit does not depend on Android, screen colors, or
  which cell is selected. Place this logic in a `domain` package.
- **A pure function** computes a result from its arguments without changing
  outside state. This validator reads the array, builds a local result, and
  returns it. It does not mutate the board, use Android logging, or access UI.
- **`object`** declares one Kotlin object. We call its function as
  `SudokuValidator.findRowConflicts(board)` without constructing an instance.
  The object stores no state between calls.
- **`Set<Int>`** contains unique integer indices. A cell repeated in several
  matching pairs should appear only once. `mutableSetOf<Int>()` lets the
  implementation accumulate indices; the return type exposes read-only set
  operations to callers.
- **A unit test** checks a small piece of behavior with known input and an
  expected result. `@Test` marks a test method; assertions fail the test if
  its actual result differs from the expected one.

Local tests in `app/src/test/` run on the workstation's JVM (Java Virtual
Machine). Instrumented tests in `app/src/androidTest/` run on an Android
device or emulator. Our validator needs only Kotlin, so a local test is enough.
See Android's [testing fundamentals](https://developer.android.com/training/testing/fundamentals)
for the distinction between local and instrumented tests.

## Implementation

### 1. Create the validator and its contract

Create `app/src/main/java/com/example/sudokusolver/domain/SudokuValidator.kt`.
Start with:

```kotlin
package com.example.sudokusolver.domain

object SudokuValidator {
    fun findRowConflicts(board: IntArray): Set<Int> {
        val conflicts = mutableSetOf<Int>()

        return conflicts
    }
}
```

This initial skeleton returns an empty result. We will fill in the calculation
next. Its input contract is the existing board convention: exactly 81 entries,
each in `0..9`. Zero means empty. Returning an empty set means no **row**
conflicts, not necessarily a valid or solved Sudoku.

The function is named `findRowConflicts` to make its current scope explicit.
Column and box checking will be introduced in Step 05B.

### 2. Visit every cell in every row

Between the set declaration and `return`, add:

```kotlin
for (row in 0 until 9) {
    for (column in 0 until 9) {
        val index = row * 9 + column
        val value = board[index]
        if (value == 0) continue
    }
}
```

The indexing formula is the same one used by the UI. `continue` skips the
rest of the current column-loop iteration when the cell is empty. Empty
cells can repeat without creating a conflict.

### 3. Compare with the remaining cells in that row

Immediately after the zero check, inside the column loop, add:

```kotlin
for (otherColumn in column + 1 until 9) {
    val otherIndex = row * 9 + otherColumn
    if (board[otherIndex] == value) {
        conflicts.add(index)
        conflicts.add(otherIndex)
    }
}
```

Only compare cells to their right. Starting at `column + 1` avoids comparing
a cell with itself and avoids checking each pair in both directions. For the
last column this range is empty, which is correct.

If two values match, add **both** indices: both cells participate in the
conflict. If three cells have the same value, several pairs match, but the set
still returns each participating index just once. Keep the set outside all
loops so conflicts from earlier rows are retained.

For example, indices `18` and `26` belong to row 2 (the third visual row).
If both contain `5`, the result is `setOf(18, 26)`. The function reports cell
positions, not the digit `5` or the row number `2`.

### 4. Create a local test class

Create `app/src/test/java/com/example/sudokusolver/domain/SudokuValidatorTest.kt`:

```kotlin
package com.example.sudokusolver.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SudokuValidatorTest {
}
```

Add each test below inside this class. The official
[Build local unit tests](https://developer.android.com/training/testing/local-tests)
guide explains the `src/test` location, JUnit 4 setup, and assertions. This
project already has the needed dependency; no mocking framework is required.

Use three stages in each test: arrange the input, call the function, and
assert the result. Exact set equality also catches unexpected extra indices;
set order is irrelevant.

Use a descriptive phrase in backticks for each test function name. Kotlin
allows spaces inside these names, making the behavior readable in both the
source and the test results. We use this naming style for the course's JVM
tests from this step onward.

### 5. Test valid rows

```kotlin
@Test
fun `valid rows have no conflicts`() {
    val board = IntArray(81) { index -> index % 9 + 1 }

    val conflicts = SudokuValidator.findRowConflicts(board)

    assertEquals(emptySet<Int>(), conflicts)
}
```

The array initializer runs for indices 0 through 80. `% 9 + 1` fills each row
with `1 2 3 4 5 6 7 8 9`. These rows are individually valid even though digits
repeat in columns and boxes. The test intentionally establishes that this
historical step checks only rows. It will need a different full-board fixture
when the validator's contract expands in Step 05B.

### 6. Test a duplicate and unchanged input

```kotlin
@Test
fun `duplicate values in a row are conflicts`() {
    val board = IntArray(81)
    board[18] = 5
    board[26] = 5
    val originalBoard = board.copyOf()

    val conflicts = SudokuValidator.findRowConflicts(board)

    assertEquals(setOf(18, 26), conflicts)
    assertArrayEquals(originalBoard, board)
}
```

`IntArray(81)` starts with zeroes, so the test needs only two assignments to
describe its puzzle. Direct mutation is appropriate while building a test
fixture: it is not published Compose state. `copyOf()` captures the input
before validation, and `assertArrayEquals` compares its contents afterwards.
This verifies that finding conflicts did not edit the puzzle.

### 7. Test empty cells

```kotlin
@Test
fun `empty cells are ignored`() {
    val board = IntArray(81)
    assertEquals(emptySet<Int>(), SudokuValidator.findRowConflicts(board))

    board[0] = 5
    board[9] = 5

    assertEquals(emptySet<Int>(), SudokuValidator.findRowConflicts(board))
}
```

First check an entirely empty board, then one containing digits among the
zeroes. The two `5`s are in different rows, so they do not conflict under this
step's rules. The partial-board fixture will change when column/box validation
is added.

### 8. Test all occurrences and accumulation across rows

Add these two cases to catch stopping at the first matching pair or resetting
the result between rows:

```kotlin
@Test
fun `all occurrences of a repeated value are conflicts`() {
    val board = IntArray(81)
    board[72] = 7
    board[75] = 7
    board[80] = 7

    val conflicts = SudokuValidator.findRowConflicts(board)

    assertEquals(setOf(72, 75, 80), conflicts)
}
```

```kotlin
@Test
fun `conflicts from different rows are collected`() {
    val board = IntArray(81)
    board[0] = 4
    board[8] = 4
    board[36] = 2
    board[40] = 2
    board[80] = 9

    val conflicts = SudokuValidator.findRowConflicts(board)

    assertEquals(setOf(0, 8, 36, 40), conflicts)
}
```

The first case includes the last cell, index 80. The second checks separate
conflicts in rows 0 and 4, while the unrelated `9` stays out of the result.

## Logging / troubleshooting

The validator has no Android logging. Its results are inspected through test
assertions, so no new Logcat output is expected. Existing UI logs still work.

- **Every filled cell conflicts:** do not start the comparison at the same
  column; begin at `column + 1` to avoid matching a cell with itself.
- **Blank cells appear as conflicts:** skip `value == 0` before comparing.
- **Only one index from a pair is returned:** add both `index` and `otherIndex`.
- **Earlier conflicts disappear:** declare the set before the row loop.
- **JUnit imports cannot be resolved:** check that the file is under
  `app/src/test/java`, then sync the project. `testImplementation(libs.junit)`
  already exists.
- **A local test reports an Android method is not mocked:** check that no
  `android.*`, Compose, or logging call was introduced into the validator.
  Android's [local-test guide](https://developer.android.com/training/testing/local-tests)
  explains why Android framework methods are not implemented in ordinary JVM
  tests. Keep this logic independent instead of adding test workarounds.
- **The app still allows duplicate digits:** expected. These tests call the
  validator directly; the screen does not call it yet.

## Run / test

In Android Studio:

1. Open `SudokuValidatorTest.kt` under `app/src/test/java`.
2. Use the gutter run icon beside the class, or right-click the class and
   choose **Run 'SudokuValidatorTest'**. These are local JVM tests; an emulator
   is not needed.
3. Confirm all five tests pass:

   | Test | Expected behavior |
   |---|---|
   | `valid rows have no conflicts` | Individually valid rows return an empty set |
   | `duplicate values in a row are conflicts` | Both indices are returned; input is unchanged |
   | `empty cells are ignored` | Empty and partially filled rows are accepted |
   | `all occurrences of a repeated value are conflicts` | All three matching cells are returned |
   | `conflicts from different rows are collected` | Both row conflicts are collected, without unrelated cells |

4. If a test fails, compare its expected and actual sets in the test results
   window before changing the code.
5. Build the app and optionally run the existing editing checks. The visible
   UI should be identical to Step 04, with no conflict highlighting yet.

## Expected result

- ✓ The domain package contains a pure Kotlin row validator.
- ✓ Repeated nonzero digits report all participating cell indices.
- ✓ Empty cells are ignored and the input board is unchanged.
- ✓ Five JVM tests pass without requiring an Android device.
- ✓ The editable UI retains its Step 04 behavior.

## Architecture after this step

```text
App: MainActivity → SudokuScreen → SudokuBoard + NumberPad
                         (unchanged; no validation call yet)

Local JVM tests
    └── SudokuValidator.findRowConflicts(IntArray)
            └── Set<Int> of conflicting cell indices
```

The validator is independent of Android and Compose. That separation makes
its rules testable before we connect the result to the UI.
