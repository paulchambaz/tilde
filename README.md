# tilde

A minimal Android launcher. Home screen shows time, date, and a short list of
favorites. Swipe down for the app drawer, swipe up for notifications.

<p align="center">
  <img src="assets/home.png" width="30%" />
  <img src="assets/drawer.png" width="30%" />
  <img src="assets/settings.png" width="30%" />
</p>

## Build

```sh
just run      # install and launch on a connected device
just build    # debug APK
just release  # release APK
just test     # unit tests
```

For wireless ADB:

```sh
just pair <ip> <port> <code>
just connect
```

## License

Tilde is free software. You can use, study, modify and distribute it under the
terms of the [GNU General Public License](https://www.gnu.org/licenses/gpl-3.0.en.html),
version 3 or later.
