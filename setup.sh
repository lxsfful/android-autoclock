#!/usr/bin/env bash
set -euo pipefail

# android-autoclock — local setup helper
# Usage: ./setup.sh

echo "=== android-autoclock setup ==="

missing=0

check_java_version() {
  if ! command -v java >/dev/null 2>&1; then
    echo "Error: Java is required. Install JDK 17+ and set JAVA_HOME."
    missing=1
    return
  fi

  java_version="$(java -version 2>&1 | sed -n 's/.*version "\([^"]*\)".*/\1/p' | sed -n '1p')"
  if [ -z "$java_version" ]; then
    echo "Warning: Could not determine Java version. Ensure JDK 17+ is installed."
    return
  fi

  case "$java_version" in
    1.*) java_major="${java_version#1.}"; java_major="${java_major%%.*}" ;;
    *) java_major="${java_version%%.*}" ;;
  esac

  if ! [ "$java_major" -ge 17 ] 2>/dev/null; then
    echo "Error: JDK 17+ is required. Detected Java version: $java_version"
    missing=1
  else
    echo "Java version OK: $java_version"
  fi
}

normalize_sdk_dir() {
  printf '%s\n' "$1" | sed 's#\\#/#g'
}

check_java_version

if [ -z "${JAVA_HOME:-}" ]; then
  echo "Warning: JAVA_HOME is not set. Gradle/Android Studio may still require it."
fi

if [ ! -x ./gradlew ]; then
  echo "Error: ./gradlew is missing or not executable."
  missing=1
fi

if [ ! -f local.properties ]; then
  sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [ -n "$sdk_dir" ]; then
    normalized_sdk_dir="$(normalize_sdk_dir "$sdk_dir")"
    printf 'sdk.dir=%s\n' "$normalized_sdk_dir" > local.properties
    echo "Created local.properties from ANDROID_HOME/ANDROID_SDK_ROOT."
  else
    cat > local.properties <<'EOF'
# Set this to your local Android SDK path before building.
# Example: sdk.dir=/path/to/Android/Sdk
sdk.dir=
EOF
    echo "Created local.properties placeholder. Edit sdk.dir before running Gradle."
    missing=1
  fi
else
  echo "local.properties already exists."
fi

if [ ! -f .env ]; then
  cp .env.example .env
  echo "Created .env from .env.example for local notes only. Do not commit .env."
else
  echo ".env already exists."
fi

if [ "$missing" -ne 0 ]; then
  echo ""
  echo "Setup checks found missing prerequisites. Install them, then run ./setup.sh again."
  exit 1
fi

echo ""
echo "=== Setup checks complete ==="
echo "Next steps:"
echo "  1. Review README.md configuration placeholders."
echo "  2. Run: ./gradlew testDebugUnitTest"
echo "  3. Run: ./gradlew assembleDebug"
echo "  4. Using Claude Code? CLAUDE.md has project context."
