# tilde

A minimal Android launcher. Three screens: swipe up for notifications, swipe
down for the app drawer. Home shows the time, date, and a list of favorites.

<p align="center">
  <img src="assets/home.png" width="30%" />
  <img src="assets/drawer.png" width="30%" />
  <img src="assets/settings.png" width="30%" />
</p>

## Install

```sh
just run
```

Requires a device connected over ADB. For wireless pairing:

```sh
just pair <ip> <port> <code>
```

## Build

```sh
just build    # debug APK
just release  # release APK
just test     # unit tests
```

## License

GPL-3.0. See [LICENSE](LICENSE).
