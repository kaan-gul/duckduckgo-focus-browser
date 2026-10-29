# Focus Mode

Time-boxed browsing sessions. The app closes when a session ends, with one short, optional extension.

## How a session starts

| Launch | Behaviour |
|---|---|
| Opened from the app icon | A duration picker blocks the browser until a duration is chosen |
| Opened from a link, shortcut or widget | A 3 minute session starts immediately, then the duration picker opens |

The picker cannot be dismissed. Back closes the app.

## Structure

- `focus-mode-impl`: the only module. Nothing outside it needs a public surface, so there is no `-api`.
  It plugs in through `ActivityLifecycleCallbacks` and does not touch `BrowserActivity`.
- `session/`: pure session logic (state machine, launch classification). No Android dependencies, so it
  is covered by plain JVM unit tests.
- `store/`, `history/`: DataStore for the session and settings (end times are absolute, so a session survives process death), Room for
  finished sessions. History keeps the 500 most recent sessions.
- `ui/`: `FocusActivityMonitor` (lifecycle callbacks, timers) and `FocusSessionPresenter` (picker sheet, extension prompt, toast, close).
- `settings/`: the settings screen, reachable from the main Settings list.
- `adskip/`: YouTube ad skipper, a `JsInjectorPlugin`.

## Settings

| Setting | Default |
|---|---|
| Allow one extension per session | on |
| Extension length | 5 min |
| Starting time for links opened from other apps | 3 min |

