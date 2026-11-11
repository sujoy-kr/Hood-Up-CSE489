#!/usr/bin/env bash
set -u
cd -- "$(dirname -- "${BASH_SOURCE[0]}")" || exit 1
export JAVA_HOME=/opt/android-studio/jbr
if [[ ! -x "$JAVA_HOME/bin/javac" || ! -x "$JAVA_HOME/bin/jlink" ]]; then
    echo 'Android Studio JDK must contain both javac and jlink.'
    exit 1
fi
mkdir -p build
./gradlew assembleDebug testDebugUnitTest --no-daemon --no-configuration-cache --console=plain --stacktrace > build/prompt7-build.log 2>&1
build_status=$?
tail -n 60 build/prompt7-build.log
printf '\nBuild exit code: %s\nFull log: %s/build/prompt7-build.log\n' "$build_status" "$PWD"
exit "$build_status"
