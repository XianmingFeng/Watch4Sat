#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage: export-public-source.sh <repository> <tree-ish> <archive-prefix> <output.tar.gz>

Exports the explicit public-source allowlist from an exact commit. The prefix
must be a relative directory ending in '/'. Existing output is replaced only
after the complete archive passes validation.
USAGE
  exit 2
}

[[ "$#" -eq 4 ]] || usage
REPOSITORY="$1"
TREEISH="$2"
ARCHIVE_PREFIX="$3"
OUTPUT="$4"
ALLOWLIST_PATH="config/public-source-allowlist.txt"

REPOSITORY="$(git -C "$REPOSITORY" rev-parse --show-toplevel)" || {
  echo "Public source repository is not a Git worktree: $1" >&2
  exit 1
}
SOURCE_COMMIT="$(git -C "$REPOSITORY" rev-parse --verify "$TREEISH^{commit}")" || {
  echo "Public source tree-ish does not resolve to a commit: $TREEISH" >&2
  exit 1
}

case "$ARCHIVE_PREFIX" in
  ""|/*|../*|*/../*|*/..|*//*|*\\*)
    echo "Public source archive prefix must be a safe relative directory." >&2
    exit 1
    ;;
  */) ;;
  *)
    echo "Public source archive prefix must end in '/'." >&2
    exit 1
    ;;
esac

git -C "$REPOSITORY" cat-file -e "$SOURCE_COMMIT:$ALLOWLIST_PATH" 2>/dev/null || {
  echo "Public source allowlist is missing from source commit: $ALLOWLIST_PATH" >&2
  exit 1
}

ALLOWLIST_CONTENT="$(git -C "$REPOSITORY" show "$SOURCE_COMMIT:$ALLOWLIST_PATH")"
ALLOWLIST=()
while IFS= read -r entry || [[ -n "$entry" ]]; do
  [[ -n "$entry" && "${entry:0:1}" != "#" ]] || continue
  if [[ "$entry" =~ ^[[:space:]] || "$entry" =~ [[:space:]]$ ]]; then
    echo "Public source allowlist entries cannot have surrounding whitespace." >&2
    exit 1
  fi
  case "$entry" in
    /*|../*|*/../*|*/..|*//*|*\\*|*'*'*|*'?'*|*'['*)
      echo "Public source allowlist entry is not an exact safe path: $entry" >&2
      exit 1
      ;;
  esac
  for existing in "${ALLOWLIST[@]:-}"; do
    [[ -n "$existing" ]] || continue
    if [[ "$entry" == "$existing" ||
      "$entry" == "$existing/"* || "$existing" == "$entry/"* ]]
    then
      echo "Public source allowlist entries overlap: $existing and $entry" >&2
      exit 1
    fi
  done
  git -C "$REPOSITORY" cat-file -e "$SOURCE_COMMIT:$entry" 2>/dev/null || {
    echo "Public source allowlist path is absent from source commit: $entry" >&2
    exit 1
  }
  ALLOWLIST+=("$entry")
done <<< "$ALLOWLIST_CONTENT"

[[ "${#ALLOWLIST[@]}" -gt 0 ]] || {
  echo "Public source allowlist is empty." >&2
  exit 1
}

REQUIRED_FILES=(
  README.md
  .gitignore
  LICENSE
  NOTICE
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
  gradlew
  gradle/libs.versions.toml
  gradle/wrapper/gradle-wrapper.jar
  gradle/wrapper/gradle-wrapper.properties
  app-wear/build.gradle.kts
  app-wear/proguard-rules.pro
  app-wear/src/main/AndroidManifest.xml
  app-wear/src/main/assets/legal/NOTICES.txt
  app-wear/src/release/generated/baselineProfiles/baseline-prof.txt
  app-wear/src/release/generated/baselineProfiles/startup-prof.txt
  benchmark/build.gradle.kts
  benchmark/src/main/AndroidManifest.xml
  core-data/build.gradle.kts
  core-data/src/test/kotlin/com/xianming/watch4sat/data/DataLayerContractTest.kt
  core-domain/build.gradle.kts
  core-domain/src/test/kotlin/com/xianming/watch4sat/domain/fixture/SyntheticOrbitFixturesTest.kt
  core-domain/src/test/resources/orbital-fixtures/README.md
  config/allowed-dependency-licenses.json
  config/public-source-allowlist.txt
  docs/licenses/LOOK4SAT-GPLv3.txt
  docs/licenses/LOOK4SAT-UPSTREAM.md
  docs/privacy-policy.md
  scripts/watch4sat-env.sh
  scripts/render_launcher_icon.py
  scripts/export-public-source.sh
)
for required in "${REQUIRED_FILES[@]}"; do
  git -C "$REPOSITORY" cat-file -e "$SOURCE_COMMIT:$required" 2>/dev/null || {
    echo "Required public source file is missing: $required" >&2
    exit 1
  }
done

SELECTED_PATHS=()
while IFS= read -r -d '' record; do
  mode="${record%% *}"
  path="${record#*$'\t'}"
  case "$mode" in
    100644|100755) ;;
    120000)
      echo "Public source export rejects symbolic links: $path" >&2
      exit 1
      ;;
    160000)
      echo "Public source export rejects submodules: $path" >&2
      exit 1
      ;;
    *)
      echo "Public source export found unsupported Git mode $mode: $path" >&2
      exit 1
      ;;
  esac
  SELECTED_PATHS+=("$path")
done < <(
  git -C "$REPOSITORY" ls-tree -r -z "$SOURCE_COMMIT" -- "${ALLOWLIST[@]}"
)

[[ "${#SELECTED_PATHS[@]}" -gt 0 ]] || {
  echo "Public source allowlist selected no files." >&2
  exit 1
}

DENIED_PATH_PATTERN='(^|/)(AGENTS\.md|CHANGELOG\.md|\.github|archive|release-artifacts|docs/(archive|plans|reviews|test-reports)|src/androidTest|\.kotlin|\.playwright-cli|build)(/|$)'
for path in "${SELECTED_PATHS[@]}"; do
  if [[ "$path" == *$'\n'* || "$path" == *$'\r'* || "$path" == *$'\t'* ]]; then
    echo "Public source export rejects control characters in paths." >&2
    exit 1
  fi
  if [[ "$path" =~ $DENIED_PATH_PATTERN ]]; then
    echo "Public source allowlist selected a denied path: $path" >&2
    exit 1
  fi
done

SENSITIVE_CONTENT_PATTERN='(/Users|/home)/[[:alnum:]_.-]+|(^|[^[:digit:]])10\.[[:digit:]]{1,3}\.[[:digit:]]{1,3}\.[[:digit:]]{1,3}([^[:digit:]]|$)|(^|[^[:digit:]])172\.(1[6-9]|2[0-9]|3[01])\.[[:digit:]]{1,3}\.[[:digit:]]{1,3}([^[:digit:]]|$)|(^|[^[:digit:]])192\.168\.[[:digit:]]{1,3}\.[[:digit:]]{1,3}([^[:digit:]]|$)|(^|[^[:xdigit:]])([[:xdigit:]]{2}:){5}[[:xdigit:]]{2}([^[:xdigit:]]|$)|(^|[^[:alnum:]_-])(emulator-[[:digit:]]{4,}|adb-[[:alnum:]_-]{8,})([^[:alnum:]_-]|$)'
set +e
SENSITIVE_FILES="$(
  git -C "$REPOSITORY" grep -a -l -E "$SENSITIVE_CONTENT_PATTERN" \
    "$SOURCE_COMMIT" -- "${ALLOWLIST[@]}" 2>/dev/null
)"
GREP_STATUS="$?"
set -e
if [[ "$GREP_STATUS" -eq 0 ]]; then
  echo "Public source export found local path, LAN address, or device identifier in:" >&2
  printf '%s\n' "$SENSITIVE_FILES" | sed 's/^[^:]*:/'"  $ARCHIVE_PREFIX"'/' >&2
  exit 1
elif [[ "$GREP_STATUS" -ne 1 ]]; then
  echo "Unable to scan public source blob content." >&2
  exit 1
fi

OUTPUT_DIRECTORY="$(dirname "$OUTPUT")"
mkdir -p "$OUTPUT_DIRECTORY"
TEMP_OUTPUT="$(mktemp "$OUTPUT_DIRECTORY/.public-source.XXXXXX.tar.gz")"
cleanup() {
  rm -f "$TEMP_OUTPUT"
}
trap cleanup EXIT

git -C "$REPOSITORY" archive \
  --format=tar \
  --prefix="$ARCHIVE_PREFIX" \
  "$SOURCE_COMMIT" \
  -- "${ALLOWLIST[@]}" |
  gzip -n > "$TEMP_OUTPUT"
gzip -t "$TEMP_OUTPUT"

ARCHIVE_ENTRIES="$(tar -tzf "$TEMP_OUTPUT")"
[[ -n "$ARCHIVE_ENTRIES" ]] || {
  echo "Public source archive is empty." >&2
  exit 1
}
while IFS= read -r archived_path; do
  [[ -n "$archived_path" ]] || continue
  case "$archived_path" in
    "$ARCHIVE_PREFIX"*) ;;
    *)
      echo "Public source archive contains a path outside its prefix." >&2
      exit 1
      ;;
  esac
  relative_path="${archived_path#"$ARCHIVE_PREFIX"}"
  case "$relative_path" in
    ../*|*/../*|*/..|/*)
      echo "Public source archive contains an unsafe path." >&2
      exit 1
      ;;
    ""|*/) continue ;;
  esac
  if [[ "$relative_path" =~ $DENIED_PATH_PATTERN ]]; then
    echo "Public source archive contains a denied path: $relative_path" >&2
    exit 1
  fi
done <<< "$ARCHIVE_ENTRIES"

mv "$TEMP_OUTPUT" "$OUTPUT"
TEMP_OUTPUT=""
trap - EXIT
echo "Exported public source from $SOURCE_COMMIT: $OUTPUT"
