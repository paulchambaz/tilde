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
