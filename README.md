# tilde

Minimalist Android launcher. Time, favorites, and a search bar — nothing else.

<p align="center">
  <img src="assets/home.png" width="30%" />
  <img src="assets/drawer.png" width="30%" />
  <img src="assets/settings.png" width="30%" />
</p>

## Gestures

| From home | Action |
|-----------|--------|
| Swipe up | Notifications |
| Swipe down | App drawer |
| Tap time | Configurable |
| Tap date | Configurable |
| Swipe left / right | Configurable |

## Build

```sh
just build   # compile debug APK
just run     # install and launch
just deploy  # release build
```

Requires a connected device or emulator. Use `just pair` for wireless ADB.
