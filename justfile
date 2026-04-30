# Install and launch the app
run: install
    adb -s $(cat .device) shell am start -n xyz.chambaz.testandroid/.MainActivity

# Compile a debug APK
build:
    ./gradlew assembleDebug

# Compile a release APK
release:
    ./gradlew assembleRelease

# Push debug APK to a connected device
install:
    ./gradlew installDebug

# Delete all build outputs
clean:
    ./gradlew clean

# Stream device logs filtered to this app
log:
    adb -s $(cat .device) logcat | grep xyz.chambaz.testandroid

# List connected devices
devices:
    adb devices

# Pair and connect
pair ip pair_port code connect_port:
    adb pair {{ip}}:{{pair_port}} {{code}}
    echo "{{ip}}:{{connect_port}}" > .device
    adb connect $(cat .device)

# Connect to saved device
connect:
    adb connect $(cat .device)

# Run unit tests
test:
    ./gradlew testDebug
