# Step 07D — Package cleanup, logging abstraction, and ViewModel tests

## Goal

Group the UI files in `ui/`, give the ViewModel a replaceable logger, and test
its actions on the JVM. Keep the existing app behavior while making its state
transitions independently testable.

## Starting point

Start from Step 07C, commit `6ee545e`
(`step-07c: migrate app to sudoku domain model`). The domain has 34 tests and
immutable board APIs. The ViewModel owns the screen's actions, but calls Android's
`Log` directly. The screen, grid, and number pad still live in the root package.

## Concepts introduced

- A **package** groups related code. Here the Activity hosts the screen, `ui/`
  contains presentation code, and `domain/` contains Sudoku rules and objects.
- An **interface** describes operations a caller needs without specifying their
  implementation. `AppLogger` promises debug, info, and warning methods.
- **Constructor dependency injection** means supplying a needed object when
  creating a class. The ViewModel receives its logger instead of calling one
  fixed logging implementation directly.
- An **adapter** translates our logging methods into Android `Log` calls.
- A **test double** substitutes a controlled implementation in tests. Our
  `FakeLogger` records messages in memory without using Android.

See Android's [dependency injection introduction](https://developer.android.com/training/dependency-injection)
for constructor injection and its [test doubles guide](https://developer.android.com/training/testing/fundamentals/test-doubles)
for replacing dependencies during tests. This small example needs only a
constructor parameter, not a dependency-injection framework.

## Implementation

### 1. Finish organizing the UI package

Move these files into `app/src/main/java/com/example/sudokusolver/ui/`:

- `SudokuScreen.kt`
- `SudokuGrid.kt`
- `NumberPad.kt`

Change their package declarations to `com.example.sudokusolver.ui`.
`SudokuCell` stays in the grid file. Remove redundant imports of `SudokuMode`,
`SudokuUiState`, and `SudokuViewModel` from the screen, since those types now
share its package.

Move `SudokuTitle` and `SudokuTitlePreview` from `MainActivity.kt` into
`ui/SudokuScreen.kt`. They belong with the screen's composables. Remove the
Activity's now-unused `Text`, `Composable`, and `Preview` imports, and add:

```kotlin
import com.example.sudokusolver.ui.SudokuScreen
```

The Activity still sets up the theme and Scaffold and logs its lifecycle. Keep
all component parameters, layout modifiers, previews, and click behavior intact.

### 2. Define the logging contract

Create `util/AppLogger.kt` in package `com.example.sudokusolver.util`:

```kotlin
interface AppLogger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warning(tag: String, message: String)
}
```

The interface only describes what the caller can do. Its arguments are ordinary
strings and its methods return `Unit`; callers do not need Android's logging
API or its integer return values.

### 3. Adapt the contract to Logcat

In the same file, import `android.util.Log` and add:

```kotlin
object AndroidAppLogger : AppLogger {
    override fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun warning(tag: String, message: String) {
        Log.w(tag, message)
    }
}
```

Kotlin's `object` declaration provides one shared instance. `override` supplies
the interface methods. This adapter preserves the existing tags, messages, and
log levels. The Activity and grid may still call Android logging directly:
they already belong to Android UI code.

### 4. Supply the logger to the ViewModel

Change the ViewModel constructor:

```kotlin
class SudokuViewModel(
    private val logger: AppLogger = AndroidAppLogger
) : ViewModel() {
    // Existing state and actions stay here.
}
```

Import `AppLogger` and `AndroidAppLogger` from `util`, then remove the
`android.util.Log` import. Replace every ViewModel log call, including the one
in `init`:

| Existing call | Replacement |
|---|---|
| `Log.d(tag, message)` | `logger.debug(tag, message)` |
| `Log.i(tag, message)` | `logger.info(tag, message)` |
| `Log.w(tag, message)` | `logger.warning(tag, message)` |

Leave state assignments, domain calls, and messages unchanged. The default
argument keeps the screen's existing `viewModel()` creation working with the
Android logger. Tests explicitly pass another implementation. The ViewModel
depends on the logging contract for its actions while retaining a convenient
Android default for this small app.

### 5. Record logs in tests

Create `app/src/test/java/com/example/sudokusolver/util/FakeLogger.kt`:

```kotlin
class FakeLogger : AppLogger {
    data class Entry(val level: String, val tag: String, val message: String)

    val entries = mutableListOf<Entry>()

    override fun debug(tag: String, message: String) {
        entries.add(Entry("DEBUG", tag, message))
    }

    override fun info(tag: String, message: String) {
        entries.add(Entry("INFO", tag, message))
    }

    override fun warning(tag: String, message: String) {
        entries.add(Entry("WARNING", tag, message))
    }
}
```

Use package `com.example.sudokusolver.util`. This file belongs to the test
source set and is not included in the application. Each recorded entry stores
enough information for a test to inspect a meaningful workflow message.

Android's [local unit test guide](https://developer.android.com/training/testing/local-tests)
explains that ordinary JVM tests cannot execute real Android framework methods.
Passing a fake avoids `Log` calls without suppressing Android errors globally.

### 6. Test actions through public state

Create `app/src/test/java/com/example/sudokusolver/ui/SudokuViewModelTest.kt`
in package `com.example.sudokusolver.ui`. Import the fake, JUnit's `Test`, and
assertions from `org.junit.Assert`.

Start the class with:

```kotlin
private val logger = FakeLogger()
private val viewModel = SudokuViewModel(logger)
```

JUnit 4 creates a fresh test-class instance for each test method, so each starts
with a fresh logger, ViewModel, and initial puzzle. The methods run synchronously
and do not launch coroutines, so these tests do not need a dispatcher rule.

Use the same arrange/action/assert sequence as the domain tests. For example:

```kotlin
@Test
fun `number is entered into selected cell`() {
    viewModel.selectCell(2)
    val previousState = viewModel.uiState

    viewModel.enterNumber(4)

    assertEquals(4, viewModel.uiState.board[2])
    assertEquals(4, viewModel.uiState.originalBoard[2])
    assertEquals(31, viewModel.uiState.originalBoard.clueCount)
    assertEquals(2, viewModel.uiState.selectedCell)
    assertEquals(0, previousState.board[2])
    assertEquals(0, previousState.originalBoard[2])
}
```

This checks displayed digits, accepted clues, selection, and preservation of
the previous state. It does not access private methods or imitate the update
implementation.

Add the following behavior checks, using backtick names throughout:

| Behavior | Action and observable result |
|---|---|
| Selection | Select index 23; selection changes and board contents stay intact. |
| Entry | Enter 4 at index 2; both boards update and previous state stays intact. |
| Clear | Clear the built-in 5 at index 0; both boards lose that clue. |
| Missing selection | Enter and Clear leave state unchanged and record warnings. |
| Conflicts | Enter 7 at index 2; conflicting indices are exactly 2 and 4. |
| Conflict removal | Clear that duplicate; conflicts disappear. |
| Solve rejection | Conflicts leave state in EDIT, record rejection, and do not log solver startup. |
| Successful solve | The board becomes complete and valid, selection clears, mode becomes SOLUTION, and clues stay intact. |
| Solution-mode guards | Selection, entry, Clear, and repeated Solve do not change the solution. |
| Unsolvable puzzle | Enter 1 at index 2; Solve leaves state unchanged and records no solution. |
| Edit clues | A manually entered 4 survives; generated digits disappear and mode returns to EDIT. |
| Reset from EDIT | Reset removes edits, conflicts, and selection. |
| Reset from SOLUTION | Reset removes the solution and returns to the built-in puzzle in EDIT. |

For both Reset tests, use a small private `assertInitialPuzzle()` test helper.
Compare both exported boards against the explicit 81-value initial fixture, then
check 30 clues, EDIT mode, null selection, and an empty conflict set. An explicit
fixture checks the promised puzzle rather than deriving the expected result
from the same constructor being tested.

Retain the existing domain tests. The ViewModel tests exercise how the real
board, validator, and solver cooperate with screen state; only logging is
replaced. They complement the domain's exact solution-value tests.

## Logging / troubleshooting

On a device, existing Logcat behavior should remain intact. Filter with
`package:com.example.sudokusolver tag:SudokuViewModel`. For the initial puzzle:

```text
D/SudokuViewModel: ViewModel created
I/SudokuViewModel: Starting Sudoku solver: clues=30
D/SudokuViewModel: Validation completed: conflicts=[]
I/SudokuViewModel: Mode changed: EDIT -> SOLUTION
I/SudokuViewModel: Sudoku solved successfully
```

During JVM tests, these messages go into `logger.entries`. Selected assertions
check warnings and major workflow events; tests do not depend on the exact order
or total count of every debug message.

- If a test reports `Method ... in android.util.Log not mocked`, verify that it
  passes `FakeLogger` and that every ViewModel call, including initialization,
  uses `logger`. Do not turn on default return values to hide Android calls.
- If the Activity cannot resolve `SudokuScreen`, update its import after moving
  the file and check the new package declaration.
- If a preview breaks after moving files, check that it references the local
  composable in `ui/`. Screen Preview still renders `SudokuScreenContent` without
  constructing a ViewModel.
- If tests compare boards with matching digits but fail, use `assertArrayEquals`
  on exported contents. The board class still uses reference equality.

## Run / test

1. In Android Studio, run `SudokuViewModelTest` using its gutter Run action.
   Expect **13 ViewModel tests** to pass without an emulator.
2. Run `CellPositionTest`, `SudokuBoardTest`, `SudokuValidatorTest`, and
   `SudokuSolverTest`. Their **34 domain tests** should still pass. Together these
   give **47 Sudoku tests**; running all local tests also includes the generated
   `ExampleUnitTest`, for **48 total**.
3. Build and inspect the screen, grid, number-pad, and title previews after the
   package moves. Verify there are no stale imports or missing components.
4. Run on the course's Android 15 / API 35 device. Check that the default logger
   still produces lifecycle, cell-click, validation, and mode-transition logs.
5. Select index 2, enter 4, and solve. Verify clue styling, disabled editing, and
   hidden number pad. Edit clues should retain the 4 and remove generated digits.
6. Verify Clear, conflicting-entry rejection, and Reset in both modes. Rotate
   in EDIT and SOLUTION and confirm that the ViewModel retains the current state.

The JVM tests inspect state and actions; they do not render Compose, execute the
Android logging adapter, or simulate Activity recreation. The manual checks
cover those boundaries. Landscape layout keeps its existing limitation, and
ViewModel state still has the Step 07A configuration-change lifetime rather
than process-death restoration.

## Expected result

- ✓ Presentation files live in `ui/`; the root Activity hosts the screen.
- ✓ The ViewModel uses an injected logging interface instead of direct `Log` calls.
- ✓ The Android logger preserves existing runtime diagnostics.
- ✓ The fake enables local tests of all required ViewModel actions.
- ✓ All 47 Sudoku tests pass, plus the generated example test.
- ✓ The application's visible behavior and rotation handling remain intact.

## Architecture after this step

```text
com.example.sudokusolver
├── MainActivity.kt
├── domain/
│   ├── CellPosition.kt
│   ├── SudokuBoard.kt
│   ├── SudokuSolver.kt
│   └── SudokuValidator.kt
├── ui/
│   ├── SudokuScreen.kt       (also title and screen content)
│   ├── SudokuGrid.kt         (also SudokuCell)
│   ├── NumberPad.kt
│   ├── SudokuMode.kt
│   ├── SudokuUiState.kt
│   ├── SudokuViewModel.kt
│   └── theme/
└── util/
    └── AppLogger.kt          (interface and Android implementation)

Compose UI -- events --> SudokuViewModel -- state --> Compose UI
                              ├── SudokuBoard
                              ├── SudokuValidator
                              ├── SudokuSolver
                              └── AppLogger
                                    ├── AndroidAppLogger (app)
                                    └── FakeLogger (tests only)
```

This completes the planned sequence through Step 07D. The application is a
manually editable, validated Sudoku solver with clear UI state ownership and
tested domain logic and ViewModel actions.
