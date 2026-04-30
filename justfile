# Install and launch the app
run: install
    adb -s $(adb-device) shell am start -n xyz.chambaz.tilde/.MainActivity

# Compile a debug APK
build:
    ./gradlew assembleDebug

# Compile a release APK
release:
    ./gradlew assembleRelease

# Install and launch a release build
deploy: release
    adb -s $(adb-device) install -r app/build/outputs/apk/release/app-release.apk
    adb -s $(adb-device) shell am start -n xyz.chambaz.tilde/.MainActivity

# Push debug APK to a connected device
install:
    ./gradlew installDebug

# Delete all build outputs
clean:
    ./gradlew clean

# Stream device logs filtered to this app
log:
    adb -s $(adb-device) logcat | grep xyz.chambaz.tilde

# List connected devices
devices:
    adb devices

# Pair wirelessly and save ip:port to .device
pair ip pair_port code:
    adb pair {{ip}}:{{pair_port}} {{code}}
    echo "{{ip}}:0" > .device
    adb-device

# Connect to saved device
connect:
    adb-device

# Run unit tests
test:
    ./gradlew testDebugUnitTest

# Capture screenshots of all three screens into assets/
screenshot:
    mkdir -p assets
    adb -s $(adb-device) shell am start -n xyz.chambaz.tilde/.MainActivity
    sleep 1
    adb -s $(adb-device) exec-out screencap -p > assets/home.png
    adb -s $(adb-device) shell input swipe 540 1400 540 200
    sleep 1
    adb -s $(adb-device) exec-out screencap -p > assets/drawer.png
    adb -s $(adb-device) shell input swipe 900 700 100 700
    sleep 1
    adb -s $(adb-device) exec-out screencap -p > assets/settings.png
    adb -s $(adb-device) shell input keyevent KEYCODE_BACK
    adb -s $(adb-device) shell am start -n xyz.chambaz.tilde/.MainActivity
