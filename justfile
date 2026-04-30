# Install and launch the app
run: install
    adb -s $(adb-device) shell am start -n xyz.chambaz.tilde/.MainActivity

# Compile a debug APK
build:
    ./gradlew assembleDebug ${AAPT2:+-Pandroid.aapt2FromMavenOverride=$AAPT2}

# Compile a release APK
release:
    ./gradlew assembleRelease ${AAPT2:+-Pandroid.aapt2FromMavenOverride=$AAPT2}

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

# Capture screenshots
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

# Bump version across all files (versionCode derived from semver)
bump version:
    #!/usr/bin/env bash
    set -euo pipefail
    current_code=$(grep 'versionCode = ' app/build.gradle.kts | grep -o '[0-9]\+')
    new_code=$((current_code + 1))
    sed -i 's/versionName = "[0-9]\+\.[0-9]\+\.[0-9]\+"/versionName = "{{version}}"/' app/build.gradle.kts
    sed -i "s/versionCode = [0-9]\+/versionCode = $new_code/" app/build.gradle.kts
    sed -i "s/versionName: [0-9]\+\.[0-9]\+\.[0-9]\+/versionName: {{version}}/" xyz.chambaz.tilde.yml
    sed -i "s/versionCode: [0-9]\+/versionCode: $new_code/" xyz.chambaz.tilde.yml
    sed -i "s/commit: v[0-9]\+\.[0-9]\+\.[0-9]\+/commit: v{{version}}/" xyz.chambaz.tilde.yml
    sed -i "s/CurrentVersion: [0-9]\+\.[0-9]\+\.[0-9]\+/CurrentVersion: {{version}}/" xyz.chambaz.tilde.yml
    sed -i "s/CurrentVersionCode: [0-9]\+/CurrentVersionCode: $new_code/" xyz.chambaz.tilde.yml

# Tag a release and publish APK to GitHub
publish tag notes: release
  git tag {{tag}}
  git push origin {{tag}}
  cp app/build/outputs/apk/release/app-release*.apk tilde-{{tag}}.apk
  gh release create {{tag}} tilde-{{tag}}.apk \
    --title "{{tag}}" \
    --notes "{{notes}}"
  rm tilde-{{tag}}.apk

# Prepare fastlane metadata images for publishing
metadata:
  mkdir -p fastlane/metadata/android/en-US/images/phoneScreenshots
  cp assets/home.png     fastlane/metadata/android/en-US/images/phoneScreenshots/1_home.png
  cp assets/drawer.png   fastlane/metadata/android/en-US/images/phoneScreenshots/2_drawer.png
  cp assets/settings.png fastlane/metadata/android/en-US/images/phoneScreenshots/3_settings.png
  magick -background none assets/icon.svg -resize 512x512 fastlane/metadata/android/en-US/images/icon.png

# create signing key
signkey:
  keytool -genkey -v -keystore ~/.android/tilde-release.jks -alias tilde -keyalg EC -keysize 256 -validity 10000 -dname "CN=Paul Chambaz, O=Tilde, C=FR"
