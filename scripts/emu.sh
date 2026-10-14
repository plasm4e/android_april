#!/usr/bin/env bash
# Dev emulator helper for the 8 GB Codespace.
#   scripts/emu.sh setup    install SDK + Android 15 image (what LineageOS 22 is based on) + AVD
#   scripts/emu.sh start    boot headless in tmux and wait for boot
#   scripts/emu.sh stop     shut it down (do this before Gradle builds: both don't fit in RAM)
#   scripts/emu.sh install [keyboard|dialer]  install an app and make it the active keyboard/phone app
#   scripts/emu.sh shot F   save a screenshot to F
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/sdk}"
export JAVA_HOME="${JAVA_HOME_21:-/usr/local/sdkman/candidates/java/21.0.12+1-ms}"
ADB="$ANDROID_HOME/platform-tools/adb"
AVD=privacy35
IMAGE="system-images;android-35;default;x86_64"
IME=org.privacyrom.keyboard/.PrivacyKeyboardService

case "${1:-}" in
setup)
  sudo chmod 666 /dev/kvm
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq tmux libx11-xcb1 libpulse0 libnss3 \
    libxcomposite1 libxcursor1 libxdamage1 libxi6 libxtst6 libgl1 libegl1 >/dev/null
  if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    tmp=$(mktemp -d)
    curl -sSLo "$tmp/clt.zip" https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip
    unzip -q "$tmp/clt.zip" -d "$tmp" && mv "$tmp/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
    rm -rf "$tmp"
  fi
  sdk="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
  yes | "$sdk" --licenses >/dev/null
  "$sdk" platform-tools emulator "platforms;android-35" "build-tools;35.0.0" "$IMAGE" >/dev/null
  echo no | "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd -n $AVD -k "$IMAGE" -d pixel_6 --force
  printf 'hw.ramSize=2048\nhw.keyboard=no\n' >> "$HOME/.android/avd/$AVD.avd/config.ini"
  ;;
start)
  sudo chmod 666 /dev/kvm
  # Gradle daemons + emulator exceed 8 GB and the emulator gets shut down: stop them first.
  # The [G] trick keeps pkill from matching this script's own command line.
  pkill -f '[G]radleDaemon' || true
  pkill -f '[K]otlinCompileDaemon' || true
  tmux has-session -t emu 2>/dev/null || tmux new-session -d -s emu \
    "$ANDROID_HOME/emulator/emulator -avd $AVD -no-window -no-audio -no-boot-anim \
     -gpu swiftshader_indirect -no-snapshot -memory 2048 > /tmp/emulator.log 2>&1"
  for _ in $(seq 1 60); do
    [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] && { echo booted; exit 0; }
    tmux has-session -t emu 2>/dev/null || { echo "emulator exited, see /tmp/emulator.log"; exit 1; }
    timeout 5 tail -f /dev/null || true
  done
  echo "boot timed out"; exit 1
  ;;
stop)
  "$ADB" emu kill 2>/dev/null || true
  tmux kill-session -t emu 2>/dev/null || true
  ;;
install)
  app="${2:-keyboard}"
  "$ADB" install -r "$(dirname "$0")/../apps/$app/build/outputs/apk/debug/$app-debug.apk"
  case "$app" in
  keyboard)
    "$ADB" shell ime enable $IME
    "$ADB" shell ime set $IME
    ;;
  dialer)
    "$ADB" shell cmd role add-role-holder android.app.role.DIALER org.privacyrom.dialer 0
    for p in CALL_PHONE READ_CALL_LOG POST_NOTIFICATIONS; do
      "$ADB" shell pm grant org.privacyrom.dialer android.permission.$p
    done
    ;;
  esac
  # The 2-core emulator is slow: skip animations and "app isn't responding" dialogs.
  for k in window_animation_scale transition_animation_scale animator_duration_scale; do
    "$ADB" shell settings put global $k 0
  done
  "$ADB" shell settings put global hide_error_dialogs 1
  ;;
shot)
  "$ADB" exec-out screencap -p > "${2:?output file}"
  ;;
*)
  sed -n 2,7p "$0"; exit 1
  ;;
esac
