# Step 03A — Logging and lifecycle troubleshooting

## Goal

Use Android Studio's Logcat to see cell clicks, selection changes, and the
Activity lifecycle. The app still displays the same selectable Sudoku board.
These diagnostics help explain what happens when you tap, leave the app,
return, or rotate the device.

## Starting point

Start from Step 03, commit `07d80b7` (`step-03: add sudoku cell selection`).
`SudokuBoard` owns a nullable `selectedCell` using `remember` and
`mutableStateOf`. Its cell callback assigns an index, and `SudokuCell` draws
the selection. `MainActivity` hosts the UI in `setContent`.

## Concepts introduced

**Android logging** sends diagnostic messages to Logcat using
`android.util.Log`. A message has a level, a tag identifying its source, and
text describing the event. We use `MainActivity` and `SudokuBoard` as tags.

| Method | Level | Appropriate use |
|---|---|---|
| `Log.d` | Debug | State details, clicks, and lifecycle diagnostics |
| `Log.i` | Info | Major workflow events, such as a completed solve in a later step |
| `Log.w` | Warning | Unexpected but recoverable conditions |
| `Log.e` | Error | Failures, optionally including an exception |

Only Debug is needed here. A normal lifecycle callback or a repeated tap is
not a warning or error.

**Side effects** are actions beyond describing the UI, such as writing a log.
A composable body may execute repeatedly during recomposition, so placing a
log directly in that body can produce repeated messages unrelated to user
events. Click logs belong in click callbacks.

**`LaunchedEffect(selectedCell)`** runs an effect when it enters the
composition and starts it again when its key changes. Here the key is the
selected index. Recomposition with the same key does not restart the effect.
Compose manages its coroutine and cancels it when it leaves the composition
or a new key replaces the old one. We only use it for one short log statement;
no background-work architecture is needed.

**The Activity lifecycle** describes how Android creates, shows, pauses, hides,
and destroys an Activity instance. These callbacks are different from
recomposition: many UI updates can happen within one resumed Activity.

## Implementation

### 1. Log cell-click events

In `SudokuBoard.kt`, import:

```kotlin
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
```

Inside the row/column loops, expand the existing `SudokuCell` click callback:

```kotlin
onClick = {
    Log.d(
        "SudokuBoard",
        "Cell clicked: index=$index, row=$row, column=$column, value=${board[index]}"
    )
    selectedCell = index
},
```

Keep the assignment after the log. The callback already has access to `row`,
`column`, and `index = row * 9 + column`. Kotlin's string interpolation inserts
their values and the puzzle value into the message. Coordinates are zero-based;
`value=0` means the tapped cell is empty.

This log runs once per click callback, including when the same cell is clicked
again. Do not put it directly in the loops or inside `SudokuCell`'s composable
body: that would log UI construction rather than clicks.

### 2. Observe selection changes

Immediately after the existing `selectedCell` state declaration, add:

```kotlin
LaunchedEffect(selectedCell) {
    Log.d("SudokuBoard", "Selected cell changed: index=$selectedCell")
}
```

The initial effect logs `index=null`: there is no selection yet. When a click
changes the selected index, Compose observes the state update and the effect
runs with its new key.

Clicking the same cell again logs the click but does not change the key, so
there is no new selection-change message. This distinguishes an **event**
from a **state change**. The effect is a view of composed state, not a complete
event history; very rapid updates can be combined before composition observes
them. The click callback is where individual clicks are recorded.

### 3. Log Activity creation

In `MainActivity.kt`, import `android.util.Log`. Add one line immediately after
the existing `super.onCreate(savedInstanceState)` call:

```kotlin
Log.d("MainActivity", "onCreate")
```

Keep the existing edge-to-edge setup and `setContent` code as they are.

### 4. Log the remaining lifecycle callbacks

Inside `MainActivity`, after `onCreate`, add these overrides:

```kotlin
override fun onStart() {
    super.onStart()
    Log.d("MainActivity", "onStart")
}

override fun onResume() {
    super.onResume()
    Log.d("MainActivity", "onResume")
}

override fun onPause() {
    super.onPause()
    Log.d("MainActivity", "onPause")
}

override fun onStop() {
    super.onStop()
    Log.d("MainActivity", "onStop")
}

override fun onDestroy() {
    super.onDestroy()
    Log.d("MainActivity", "onDestroy")
}
```

Each override calls its superclass implementation to preserve the Activity's
normal behavior. The methods must be inside the class, not inside `onCreate`
or a composable.

| Callback | What it tells us |
|---|---|
| `onCreate` | Android is creating an Activity instance; we set up its UI |
| `onStart` | The Activity is becoming visible |
| `onResume` | The Activity enters its resumed, interactive state |
| `onPause` | The Activity leaves its resumed state; it may still be visible |
| `onStop` | The Activity is no longer visible |
| `onDestroy` | This instance is being destroyed, for example during recreation |

Android can terminate a process without calling `onDestroy`, so its absence
does not prove the Activity or process is still alive.

## Logging / troubleshooting

Open **View → Tool Windows → Logcat** in Android Studio, select the running
device, and filter for this package and Debug messages. For example:

```text
package:com.example.sudokusolver tag:SudokuBoard level:DEBUG
```

Switch the tag to `MainActivity` to focus on lifecycle messages, or remove
the tag filter to see both sources. The examples below abbreviate the Logcat
columns as `level/tag: message`; Android Studio also displays metadata such
as timestamps and process IDs.

On a fresh board composition:

```text
D/SudokuBoard: Selected cell changed: index=null
```

Tap the sixth cell in the third row (zero-based row `2`, column `5`), then
pause long enough to observe the log:

```text
D/SudokuBoard: Cell clicked: index=23, row=2, column=5, value=0
D/SudokuBoard: Selected cell changed: index=23
```

Tap that same cell again:

```text
D/SudokuBoard: Cell clicked: index=23, row=2, column=5, value=0
```

For a normal fresh launch, the lifecycle messages are:

```text
D/MainActivity: onCreate
D/MainActivity: onStart
D/MainActivity: onResume
```

Pressing Home normally produces `onPause` then `onStop`. Returning to the
existing instance produces `onStart` then `onResume`. Android also calls
`onRestart` on that return path, but this step does not log it. If Android
recreates the Activity instead, expect `onCreate` as well.

A normal rotation that recreates the Activity typically produces:

```text
D/MainActivity: onPause
D/MainActivity: onStop
D/MainActivity: onDestroy
D/MainActivity: onCreate
D/MainActivity: onStart
D/MainActivity: onResume
```

The new composition also logs `Selected cell changed: index=null`. Selection
resets because it still uses `remember`. Board-effect messages can interleave
with lifecycle messages; do not depend on their exact ordering relative to
each other.

Common problems:

- **No messages:** select the correct device and package, run a debug build,
  and ensure the level filter includes Debug rather than only Info or higher.
- **Messages for all 81 cells on launch:** the click log is outside the
  callback. Move it into `onClick`.
- **Selection logs on unrelated recompositions:** use the effect shown above,
  rather than a direct log in the composable body.
- **No selection log on a repeated tap:** expected; the selected index did not
  change. Check the click log instead.
- **Rotation logs differ:** confirm that rotation is enabled and the device
  actually changes orientation. Backgrounding, multi-window behavior, and
  process recreation can produce different lifecycle paths.

## Run / test

The instructor performs these checks in Android Studio:

1. Build and run on the Android 15 / API 35 device. Confirm the UI and selection
   still behave as in Step 03.
2. Filter Logcat by the app package. Observe launch lifecycle messages and the
   initial `index=null` selection message.
3. Tap index 23, wait, then tap it again. Compare the messages with the examples
   above: two click logs, but only one selection change to index 23.
4. Tap the top-left `5`. Expect `index=0, row=0, column=0, value=5` and a
   selection change to zero.
5. Leave the app untouched briefly. It should not continuously emit these
   diagnostic logs.
6. Press Home and return. Inspect pause/stop/start/resume messages; if the same
   Activity instance remains, the selection should remain too.
7. Rotate and return to portrait. Confirm Activity recreation logs and the
   reset selection. The width-based landscape layout remains unchanged.

No JVM unit tests are added for these Android logging callbacks. The relevant
checks are the instructor's runtime behavior and Logcat observations.

## Expected result

- ✓ Every cell click reports its index, coordinates, and value.
- ✓ The effect reports initial selection and changes to the selected index.
- ✓ Repeated taps demonstrate the difference between clicks and state changes.
- ✓ Activity lifecycle messages explain leaving, returning, and recreation.
- ✓ Idle UI does not produce a stream of repeated diagnostic messages.

## Architecture after this step

```text
MainActivity lifecycle callbacks ── Log.d("MainActivity", ...) ──┐
                                                             │
SudokuBoard                                                  ├── Logcat
    ├── onClick → click log → selectedCell update              │
    └── LaunchedEffect(selectedCell) → selection log ──────────┘
```

Selection state remains in the board. Logging observes events and state
without changing the UI structure or adding application behavior.
