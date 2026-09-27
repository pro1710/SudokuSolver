# Step 01 — Empty Compose application

## Goal

Replace the generated greeting with **Sudoku Solver**. Learn how the Activity,
theme, and Compose functions work together to display this single line of text.
There is no Sudoku board in this step.

## Starting point

Start from commit `6089f5f` (`initial android project`). This is the generated
Kotlin Empty Activity project with package `com.example.sudokusolver`.
`MainActivity.kt` contains `Greeting`, which displays `Hello Android!`, and
`GreetingPreview`. Keep the generated theme and resources.

The baseline has `compileSdk = 37`, `targetSdk = 37`, and `minSdk = 35`.
This step adjusts the minimum and target SDK settings for the teaching setup.

## Concepts introduced

- **`ComponentActivity`** is the Android Activity base class used here. An
  Activity hosts the app's screen. Android calls `onCreate` when creating an
  Activity instance; `super.onCreate(savedInstanceState)` performs the base
  class's initialization.
- **`setContent`** supplies the Activity's Compose user interface. Instead of
  referencing an XML screen layout, this app calls composable functions.
- **`@Composable`** marks a function that can describe part of the UI. Such
  functions are called from a Compose context, such as the `setContent` block
  or another composable. `Text` is an existing composable that draws text.
- **`Modifier`** configures layout and appearance. `Modifier.fillMaxSize()`
  asks for the available space; `Modifier.padding(...)` adds spacing. A
  `modifier: Modifier = Modifier` parameter lets a caller supply these choices
  while keeping the default free of extra layout instructions.
- **`Scaffold`** supplies a Material screen structure with slots for elements
  such as app bars and content. This step uses only its content area. Its
  `innerPadding` tells the content how much spacing to apply around it.
- **The theme** supplies Material colors and typography. The existing
  `SudokuSolverTheme` stays around the UI.

Useful project locations:

| Location | Purpose |
|---|---|
| `app/src/main/java/com/example/sudokusolver/MainActivity.kt` | Activity, title composable, and preview |
| `app/src/main/java/com/example/sudokusolver/ui/theme/` | Generated colors, typography, and theme |
| `app/src/main/AndroidManifest.xml` | Declares the application and launcher Activity |
| `app/src/main/res/` | Resources such as launcher icons and strings |
| `app/build.gradle.kts` | App SDK settings and dependencies |
| `gradle/libs.versions.toml` | Shared dependency and plugin versions |
| `app/src/test/` and `app/src/androidTest/` | Generated local and device test examples |

## Implementation

### 1. Set the teaching SDK values

In `app/build.gradle.kts`, change these two entries inside `defaultConfig`:

```kotlin
minSdk = 26
targetSdk = 35
```

`minSdk` is the lowest Android API level the app declares it supports.
`targetSdk` declares the Android API level whose behavior the app targets;
API 35 corresponds to Android 15. It does not specify the maximum Android
version on which the app can run.

Keep `compileSdk` at 37. This setting selects the Android APIs available to the
compiler; it does not require the teaching emulator to run API 37. Keep the
existing Gradle, plugin, and library versions. Sync the project in Android Studio
after editing the SDK settings.

### 2. Replace the generated greeting composable

In `MainActivity.kt`, rename `Greeting` to `SudokuTitle`, remove its `name`
parameter, and replace the interpolated greeting with a fixed title:

```kotlin
@Composable
fun SudokuTitle(modifier: Modifier = Modifier) {
    Text(
        text = "Sudoku Solver",
        modifier = modifier
    )
}
```

The name parameter is no longer useful because this screen has one fixed title.
Keep the modifier parameter and pass it to `Text`, so the caller's padding
actually reaches the displayed content.

### 3. Update the Activity's call

Inside the existing `Scaffold` content block, replace the call to `Greeting`
with:

```kotlin
SudokuTitle(
    modifier = Modifier.padding(innerPadding)
)
```

Keep `ComponentActivity`, `onCreate`, `enableEdgeToEdge()`, `setContent`,
`SudokuSolverTheme`, and `Scaffold(modifier = Modifier.fillMaxSize())` as
generated. Edge-to-edge drawing lets the window extend behind system bars;
applying the Scaffold's content padding helps keep the text clear of them.

### 4. Update the preview

Rename `GreetingPreview` to `SudokuTitlePreview` and replace its inner call:

```kotlin
@Preview(showBackground = true)
@Composable
fun SudokuTitlePreview() {
    SudokuSolverTheme {
        SudokuTitle()
    }
}
```

The preview renders the title component in Android Studio. It does not include
the Activity's Scaffold, so use a running device to check full-screen spacing.

## Logging / troubleshooting

This step adds no custom logs. Lifecycle and interaction logging come in Step
03A. Android Studio's Build output is useful for compile or sync errors.

- **Unresolved `Greeting` or an unexpected `name` argument:** update both the
  Activity call and the preview after changing the function signature.
- **Text appears under a system bar:** check that the Activity passes
  `Modifier.padding(innerPadding)` and `SudokuTitle` forwards it to `Text`.
- **Preview has not refreshed:** complete Gradle sync and build, then refresh
  the preview. A preview alone does not verify the running Activity.
- **SDK or dependency compatibility error:** record the exact Gradle message
  for the instructor. Keep this step's changes limited; do not attempt broad
  dependency upgrades to fix an unrelated environment problem.

## Run / test

The instructor performs these checks in Android Studio:

1. Sync the project and build the `app` module with the changed SDK settings.
2. Open `MainActivity.kt` and render `SudokuTitlePreview`. Expect the text
   `Sudoku Solver` with the generated theme.
3. Select an Android 15 / API 35 virtual device and run the app. An emulator is
   a virtual Android device; its system image determines the Android version
   being tested.
4. Confirm the app opens, displays `Sudoku Solver` without the old greeting,
   and keeps the title clear of system bars. The simple title is near the top
   of the screen; this step does not center it or add a toolbar.

There are no new unit tests for this text-only UI change. The generated example
tests remain unchanged and do not verify the title. Build success and runtime
behavior must be confirmed in Android Studio before approving the commit.

## Expected result

- ✓ The app displays `Sudoku Solver`.
- ✓ The title component has a working Compose preview.
- ✓ The generated theme remains in use.
- ✓ The app uses `minSdk = 26` and `targetSdk = 35`.
- ✓ No Sudoku functionality has been introduced.

## Architecture after this step

```text
MainActivity : ComponentActivity
    └── setContent
        └── SudokuSolverTheme
            └── Scaffold
                └── SudokuTitle
                    └── Text("Sudoku Solver")
```

The title and preview still live in `MainActivity.kt`. A separate screen or
state-owning class is unnecessary for a single static text element.

## What we deliberately do NOT implement yet

- No Sudoku board, cells, or grid lines.
- No selection, editable state, or number pad.
- No validation or solver.
- No custom lifecycle logging or ViewModel.
- No camera, OCR, or additional libraries.

The next step introduces a static 9×9 board. Stop here and verify this step
before continuing.
