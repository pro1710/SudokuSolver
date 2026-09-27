# Step 07B — Introduce a Sudoku domain model

## Goal

Represent a cell location with `CellPosition` and a puzzle with a domain
`SudokuBoard`. These objects enforce valid coordinates, array size, and digit
ranges, and let callers create edited boards without changing existing ones.
Unit tests demonstrate those guarantees.

## Starting point

Start from Step 07A, commit `8051e24`
(`step-07a: move sudoku state and actions to viewmodel`). The ViewModel owns
screen state and actions. The UI, validator, and solver still exchange arrays.
Every caller must remember that a board needs 81 values and must copy an array
before editing it.

This step adds and tests the domain objects separately. The running application
continues using its existing arrays; migrating its state and domain APIs is the
next step. Keeping these changes separate makes the model's contract reviewable
before other components depend on it.

## Concepts introduced

- A **domain object** represents something in the problem being solved, such as
  a Sudoku cell position, without depending on Android or Compose.
- **Encapsulation** hides the mutable storage behind a small public API. Callers
  ask a board for values rather than editing its array directly.
- An **invariant** is a condition every instance must satisfy: coordinates in
  `0..8`, exactly 81 cells, and values in `0..9`.
- `init` runs during construction. `require(...)` rejects an invalid argument
  with `IllegalArgumentException`, so an invalid instance is not returned.
- **Defensive copying** prevents a caller's mutable array from becoming a way
  to modify a board later.
- An **immutable update** returns a new object while preserving the previous
  object's contents.

For a refresher on Kotlin classes and properties, see Android's
[Kotlin language introduction](https://developer.android.com/kotlin/learn).

## Implementation

### 1. Give coordinates a name and a valid range

Create `domain/CellPosition.kt` with package
`com.example.sudokusolver.domain`:

```kotlin
data class CellPosition(
    val row: Int,
    val column: Int
) {
    init {
        require(row in 0..8) { "Row must be in 0..8" }
        require(column in 0..8) { "Column must be in 0..8" }
    }
}
```

Both coordinates use zero-based numbering. `CellPosition(2, 5)` means the third
row and sixth column. A data class provides value-based equality, so two
positions with those coordinates compare equal. Read Kotlin's official
[data class reference](https://kotlinlang.org/docs/data-classes.html) for its
generated functions.

Add a computed property inside the class:

```kotlin
val index: Int
    get() = row * 9 + column
```

This keeps the indexing formula in one place for users of the model. The example
above gives `2 * 9 + 5 = 23`.

Then add the reverse conversion inside a `companion object`:

```kotlin
companion object {
    fun fromIndex(index: Int): CellPosition {
        require(index in 0..80) { "Index must be in 0..80" }
        return CellPosition(row = index / 9, column = index % 9)
    }
}
```

A companion object lets us call `CellPosition.fromIndex(23)` without an existing
position. Integer division finds the row; remainder finds the column. Validate
the input first rather than converting an invalid index into coordinates.

### 2. Own the board's storage

Create `domain/SudokuBoard.kt` in the same package:

```kotlin
class SudokuBoard(values: IntArray = IntArray(81)) {
    private val cells = values.copyOf()

    init {
        require(cells.size == 81) { "A Sudoku board must contain 81 cells" }
        require(cells.all { value -> value in 0..9 }) { "Cell values must be in 0..9" }
    }
}
```

`SudokuBoard()` creates an empty puzzle. Supplying an array initializes its
values. Copying that array means a caller can later change its own array without
changing the board. Validate the copy the board actually owns.

`private val` prevents public access or reassignment, but arrays themselves are
still mutable. The following methods complete the protection by never returning
the internal array or modifying it after construction.

Use a regular class here: we want explicit control of the array API. Board
equality is not defined by contents in this step; compare exported contents with
`assertArrayEquals` in tests rather than comparing board instances.

### 3. Support readable cell lookup

Add these methods inside the board class:

```kotlin
operator fun get(index: Int): Int {
    require(index in 0..80) { "Index must be in 0..80" }
    return cells[index]
}

operator fun get(position: CellPosition): Int = get(position.index)
```

`operator` enables bracket syntax: `board[23]` and
`board[CellPosition(2, 5)]` both read the same cell. The two methods are overloads:
Kotlin chooses one using the argument's type. The coordinate overload delegates
to the index overload to keep the lookup behavior consistent. See Kotlin's
[indexed access operator documentation](https://kotlinlang.org/docs/operator-overloading.html#indexed-access-operator).

### 4. Return an updated board

Add an index-based update:

```kotlin
fun withValue(index: Int, value: Int): SudokuBoard {
    require(index in 0..80) { "Index must be in 0..80" }
    require(value in 0..9) { "Cell value must be in 0..9" }
    val updatedCells = cells.copyOf()
    updatedCells[index] = value
    return SudokuBoard(updatedCells)
}
```

The original board's array is untouched. The constructor copies the new working
array again; with only 81 integers this keeps ownership simple and explicit.
Zero is a valid update and clears a cell.

Add the coordinate overload:

```kotlin
fun withValue(position: CellPosition, value: Int): SudokuBoard =
    withValue(position.index, value)
```

For example:

```kotlin
val original = SudokuBoard()
val edited = original.withValue(CellPosition(2, 5), 7)
// original[23] is still 0; edited[23] is 7.
```

There is no `set` operator. An expression such as `board[23] = 7` should not
compile: callers must retain the new board returned by `withValue`.

### 5. Expose useful information and a safe array bridge

Add computed properties and an export method:

```kotlin
val clueCount: Int
    get() = cells.count { value -> value != 0 }

val isComplete: Boolean
    get() = cells.all { value -> value != 0 }

fun toIntArray(): IntArray = cells.copyOf()
```

`clueCount` counts occupied cells in this board; it does not track whether a
digit was typed or generated by the solver. `isComplete` means every cell is
filled, not that the puzzle obeys Sudoku rules. For example, 81 ones form a
complete but conflicting board. The validator retains responsibility for
row, column, and box conflicts.

The exported array is a copy. Code can temporarily bridge to the current
validator with `SudokuValidator.findConflicts(board.toIntArray())`. A solver
working on that exported array would modify only the copy. These are examples
of interoperability, not changes to the running app in this commit.

Keep the existing `SudokuBoard` composable in its current package. It is a
function named `com.example.sudokusolver.SudokuBoard`; the new class is
`com.example.sudokusolver.domain.SudokuBoard`. The domain tests use the domain
package, so their constructors resolve to the new model.

### 6. Test contracts through the public API

Create `CellPositionTest.kt` and `SudokuBoardTest.kt` under
`app/src/test/java/com/example/sudokusolver/domain/`. Use JUnit's `@Test` and
readable backtick names, matching the existing tests.

For coordinates, check known conversions including the asymmetric example
`(2, 5) ↔ 23`, both corners, all 81 round trips, and invalid rows, columns,
and indices. A round trip alone could miss two incorrect formulas that happen
to reverse each other, so retain the explicit known answers too.

For board updates, check both the new contents and the preserved original:

```kotlin
@Test
fun `updating an index leaves the original board unchanged`() {
    val original = SudokuBoard().withValue(0, 5)
    val expected = original.toIntArray()

    val updated = original.withValue(23, 7)

    assertArrayEquals(expected, original.toIntArray())
    expected[23] = 7
    assertArrayEquals(expected, updated.toIntArray())
    assertEquals(1, original.clueCount)
    assertEquals(2, updated.clueCount)
}
```

Test defensive copying in both directions: mutate the constructor's input array,
then separately mutate an exported array. Neither mutation should change the
board or its clue count. Also test an empty board, both lookup overloads,
coordinate updates, clearing, every digit, and filled versus incomplete boards.

Use the existing JUnit 4.13.2 `assertThrows` for invalid inputs, for example:

```kotlin
assertThrows(IllegalArgumentException::class.java) {
    SudokuBoard(IntArray(80))
}
```

Cover short and long arrays, values below zero and above nine, and index bounds
for reading and updating. Import assertions from `org.junit.Assert` and `Test`
from `org.junit.Test`. No new dependency or Android test runner is needed.
Android's [local unit test guide](https://developer.android.com/training/testing/local-tests)
explains why these tests belong in `src/test`: they run on the workstation's JVM
and do not need an emulator.

## Logging / troubleshooting

The models use pure Kotlin and produce no Logcat messages. Expected failures
appear in unit tests as `IllegalArgumentException`; `assertThrows` makes those
expected exceptions passing checks. Runtime app logs stay as they were in Step 07A.

- If index 23 produces row 5, column 2, check division and remainder order.
- If changing an input array changes a board, restore the constructor's copy.
- If changing an exported array changes a board, return a copy from `toIntArray`.
- If an update appears to do nothing, retain the result of `withValue`.
- If a board equality assertion fails despite matching digits, compare array
  contents; this regular class does not define content-based equality.
- If `isComplete` accepts a conflicting filled board, that is expected. Fullness
  and conflict detection are separate questions.

## Run / test

1. In Android Studio, run `CellPositionTest` and `SudokuBoardTest` using each
   class's gutter Run action. Expect 6 coordinate tests and 14 board tests to pass.
2. Run the existing `SudokuValidatorTest` and `SudokuSolverTest` classes too.
   Together the four domain test classes contain **33 tests**. Their existing
   array-based APIs and tests should work unchanged.
3. Build and run the application on the course's Android 15 / API 35 device.
   Its screen should be unchanged because it still uses the Step 07A state model.
4. Smoke-test selecting a cell, entering and clearing a digit, solving, Edit
   clues, and Reset. Verify normal rotation still preserves ViewModel state.

## Expected result

- ✓ Valid coordinates convert to and from indices correctly.
- ✓ Invalid coordinates, board sizes, and digit values are rejected.
- ✓ Lookup supports both an index and a `CellPosition`.
- ✓ Updating or clearing returns a new board and preserves the old one.
- ✓ Input and exported arrays cannot be used to mutate a board.
- ✓ Clue count and completeness describe the board's contents.
- ✓ All 33 domain tests pass; existing app behavior is unchanged.

## Architecture after this step

```text
Running application (unchanged):
Compose UI → SudokuViewModel → IntArray-based validator and solver

New, independently tested domain objects:
CellPosition → row / column / index
SudokuBoard  → private IntArray, lookup, copied updates, copied export
```

The next step can migrate consumers to this tested model. Keeping the model
introduction separate lets us review encapsulation and invariants before
changing the validator, solver, ViewModel, and Compose integration.
