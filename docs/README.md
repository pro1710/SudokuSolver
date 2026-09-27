# Sudoku Solver — incremental Android course

Build a Sudoku Solver one small, reviewable step at a time. This course assumes
basic programming knowledge and introduces Android, Kotlin, and Jetpack Compose
concepts as they become useful.

Each educational commit includes its code, a tutorial, and an updated index.
The instructor builds, runs, and reviews each step in Android Studio before
approving its commit. Only completed step implementations are listed below;
new tutorials and links are added as the course progresses.

| Step | Topic | Main concepts | Commit message |
|---|---|---|---|
| [01](steps/01-compose-project.md) | Compose project | ComponentActivity, setContent, Composable, Modifier, Scaffold | `step-01: simplify compose project` |
| [02](steps/02-static-sudoku-board.md) | Static Sudoku board | IntArray, row/column indexing, Row, Column, Box, weight, Canvas | `step-02: add static sudoku board` |
| [03](steps/03-cell-selection.md) | Cell selection | State, remember, mutableStateOf, recomposition, callbacks | `step-03: add sudoku cell selection` |
| [03A](steps/03a-logging.md) | Logging and lifecycle troubleshooting | Logcat, log levels, LaunchedEffect, Activity lifecycle | `step-03a: add logging and lifecycle diagnostics` |
| [04](steps/04-editable-board.md) | Editable board and number pad | State hoisting, callbacks, unidirectional data flow, copied-array updates | `step-04: add editable board and number pad` |
| [05A](steps/05a-row-validation.md) | Row validation | Pure Kotlin logic, sets, pair comparison, local JVM unit tests | `step-05a: add row validation with unit tests` |
| [05B](steps/05b-full-validation.md) | Full Sudoku validation | Shared helper, row/column/box indices, set union, regression tests | `step-05b: validate rows columns and boxes` |
| [05C](steps/05c-conflict-ui.md) | Conflict highlighting | Derived values, remember keys, Material error colors, validation logging | `step-05c: highlight sudoku conflicts in ui` |
| [06A](steps/06a-sudoku-solver.md) | Sudoku solver | Recursion, candidate checks, backtracking, mutable working array, JVM tests | `step-06a: implement sudoku backtracking solver` |
| [06B](steps/06b-solver-ui.md) | Solver UI integration | Working copies, clue tracking, conditional actions, text styling | `step-06b: integrate solver with compose ui` |
| [06C](steps/06c-edit-solution-mode.md) | Edit and Solution modes | Enum, state transitions, conditional UI, reset, rememberSaveable | `step-06c: add edit and solution modes` |

The generated project baseline is commit `6089f5f` (`initial android project`).
It is the starting point, not an educational implementation step. The table uses
commit messages, not Git tags. Before instructor approval, the newest step may
still be an uncommitted working-tree change.

## Learning workflow

```text
checkout the previous commit (starting point)
    ↓
read the corresponding tutorial for the next step
    ↓
reproduce/change the code manually
    ↓
run in Android Studio
    ↓
compare with the completed step's commit
```

Use a separate practice clone or branch for exercises and save your work before
switching commits. Find each step's hash with `git log --oneline`; for Step 01,
start from the baseline above. After reproducing a step, compare your changes
with its commit using Android Studio's Git log or `git show <step-hash>`.
Check out the completed step when you want to inspect or run the reference
implementation, after saving your exercise changes.

The aim is to understand why each change is needed and reproduce it yourself.
Later concepts are introduced in later commits, so follow the tutorials in order.

Starting with Step 04, each tutorial links to relevant official Android
documentation alongside the concepts it explains. Use those links for deeper
reading, focusing on the sections relevant to the current step.

## Teaching setup

- Kotlin and Jetpack Compose, package `com.example.sudokusolver`.
- Android Studio for Gradle sync, previews, builds, and running the app.
- Android 15 / API 35 virtual device for the course's runtime checks.
- From Step 01: `minSdk = 26`, `targetSdk = 35`; the generated project's
  `compileSdk = 37` and existing dependency versions are retained.

The compile SDK and the virtual device's Android version serve different roles:
the project compiles against its configured SDK, while the app runs on the
selected device. Follow each tutorial's run/test checklist before moving on.
