{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    {
      self,
      nixpkgs,
      flake-utils,
    }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.allowUnfree = true;
          config.android_sdk.accept_license = true; # mandatory
        };

        androidComposition = pkgs.androidenv.composeAndroidPackages {
          cmdLineToolsVersion = "13.0";
          platformToolsVersion = "35.0.2";
          buildToolsVersions = [
            "35.0.0"
            "34.0.0"
          ];
          platformVersions = [
            "35"
            "34"
          ];

          includeNDK = true;
          ndkVersions = [ "29.0.14206865" ];

          includeEmulator = true;
          includeSources = false;

          systemImageTypes = [ "google_apis" ];
          abiVersions = [ "x86_64" ];
        };

        androidSdk = androidComposition.androidsdk;
      in
      {
        devShells.default = pkgs.mkShell {
          packages = with pkgs; [
            androidSdk
            jdk21
            gradle

            kotlin

            android-tools

            just
            gnumake
            git

            kotlin-language-server
            ktfmt
            lemminx
          ];

          env = {
            ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
            ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
            JAVA_HOME = "${pkgs.jdk21}";

            GRADLE_OPTS = "-Dorg.gradle.daemon=true -Xmx4g -Dorg.gradle.jvmargs=-Xmx4g";
          };

          shellHook = ''
            export ANDROID_HOME="${androidSdk}/libexec/android-sdk"
            export ANDROID_SDK_ROOT="$ANDROID_HOME"
            export JAVA_HOME="${pkgs.jdk21}"
            export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/35.0.0:$PATH"

            # Fix NixOS aapt2 — tell Gradle to use the nix-patched binary instead of downloading one
            export AAPT2="$ANDROID_HOME/build-tools/35.0.0/aapt2"

            # Auto-generate gradle.properties for this project
            cat > gradle.properties << EOF
            android.useAndroidX=true
            android.suppressUnsupportedCompileSdk=35
            android.aapt2FromMavenOverride=$AAPT2
            org.gradle.jvmargs=-Xmx4g
            EOF
          '';
        };
      }
    );
}
