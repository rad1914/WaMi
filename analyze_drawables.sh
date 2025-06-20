# analyze_drawables.sh (fixed)
# Finds unused and duplicate drawables in an Android module (POSIX-compatible).

set -eu

# === Auto-detect Android module ===
# Look for a directory matching */src/main/res and strip off the suffix.
ANDROID_MODULE_DIR=$(find . -type d -path '*/src/main/res' | head -n1 | sed 's|/src/main/res||')
if [ -z "$ANDROID_MODULE_DIR" ]; then
  echo "❌ Could not find an Android module (*/src/main/res)" >&2
  exit 1
fi

# === Configuration ===
CODE_DIRS="$ANDROID_MODULE_DIR/src/main"             # directories to search for references
RES_DIRS=$(find "$ANDROID_MODULE_DIR/src/main/res" -type d -name "drawable*" -not -name "*.*")
OUTPUT_DIR="build/drawable_analysis"
UNUSED_OUT="$OUTPUT_DIR/unused_drawables.txt"
DUPLICATES_OUT="$OUTPUT_DIR/duplicate_groups.txt"
TMP_HASH_LIST="$OUTPUT_DIR/all_hashes.txt"

# === Options ===
DRY_RUN=false
DELETE_UNUSED=false
VERBOSE=false

usage() {
  echo "Usage: $0 [options]"
  echo "Options:"
  echo "  -n            Dry run (don't delete or modify files)"
  echo "  -d            Delete unused drawables after analysis"
  echo "  -v            Verbose output"
  echo "  -h            Show this help message"
  exit 1
}

# Parse flags
while getopts "ndvh" opt; do
  case "$opt" in
    n) DRY_RUN=true ;;
    d) DELETE_UNUSED=true ;;
    v) VERBOSE=true ;;
    h) usage ;;
    *) usage ;;
  esac
done
shift $((OPTIND -1))

# Check required tools
command -v grep >/dev/null || {
  echo "❌ grep is required but not found in PATH" >&2
  exit 1
}

# Logging helper
log() {
  [ "$VERBOSE" = true ] && echo "[LOG] $*"
}

# Cross-platform MD5
get_md5() {
  if command -v md5sum >/dev/null 2>&1; then
    md5sum "$1" | awk '{print $1}'
  elif command -v md5 >/dev/null 2>&1; then
    md5 -q "$1"
  else
    echo "❌ Error: md5sum or md5 not found." >&2
    exit 1
  fi
}

# Ensure drawable dirs found
if [ -z "$RES_DIRS" ]; then
  echo "❌ No drawable directories found under $ANDROID_MODULE_DIR/src/main/res." >&2
  exit 1
fi

# Prepare output
mkdir -p "$OUTPUT_DIR"
: > "$UNUSED_OUT"
: > "$DUPLICATES_OUT"
: > "$TMP_HASH_LIST"

echo "🔍 Scanning for unused drawables…"

# === Part 1: Unused drawables ===
for RES_DIR in $RES_DIRS; do
  for filepath in "$RES_DIR"/*; do
    [ -f "$filepath" ] || continue
    filename=$(basename "$filepath")
    base="${filename%.*}"

    count_xml=$(grep -R --include="*.xml" -c "@drawable/$base" $CODE_DIRS 2>/dev/null || echo 0)
    count_java=$(grep -R --include="*.java" -c "R.drawable.$base" $CODE_DIRS 2>/dev/null || echo 0)
    count_kt=$(grep -R --include="*.kt" -c "R.drawable.$base" $CODE_DIRS 2>/dev/null || echo 0)

    total=$((count_xml + count_java + count_kt))
    log "Checked $filename → references: $total"

    [ "$total" -eq 0 ] && echo "$filepath" >> "$UNUSED_OUT"
  done
done

echo "✓ Unused drawables → $UNUSED_OUT"
echo

echo "🔍 Scanning for duplicate drawables (by content hash)…"

# === Part 2: Duplicate drawables ===
for RES_DIR in $RES_DIRS; do
  find "$RES_DIR" -type f | while read -r file; do
    h=$(get_md5 "$file")
    echo "$h  $file"
  done
done >> "$TMP_HASH_LIST"

# Sort and group duplicates
sort "$TMP_HASH_LIST" | awk -v dup_out="$DUPLICATES_OUT" '
  {
    hash = $1; file = $2;
    if (hash == prev_hash) {
      group = group "\n  - " file
    } else {
      if (NR > 1 && prev_group != "") {
        print "Group (md5: " prev_hash "):" prev_group "\n" >> dup_out
      }
      group = "\n  - " file
    }
    prev_hash = hash; prev_group = group
  }
  END {
    if (prev_group != "") {
      print "Group (md5: " prev_hash "):" prev_group "\n" >> dup_out
    }
  }'

echo "✓ Duplicate groups → $DUPLICATES_OUT"
echo

unused_count=$(wc -l < "$UNUSED_OUT" | tr -d ' ')
duplicate_count=$(grep -c '^Group' "$DUPLICATES_OUT")

echo "===== Analysis Complete ====="
echo "• Unused drawables:   $unused_count"
echo "• Duplicate groups:   $duplicate_count"
echo

# Optional deletion
if [ "$DELETE_UNUSED" = true ]; then
  echo "⚠️  Delete unused drawables requested"
  if [ "$DRY_RUN" = false ]; then
    while IFS= read -r filepath; do
      [ -f "$filepath" ] && rm "$filepath" && log "Deleted $filepath"
    done < "$UNUSED_OUT"
    echo "✓ Deleted unused drawables."
  else
    echo "(Dry run) Files that would be deleted:"
    cat "$UNUSED_OUT"
  fi
fi

echo
  echo "Review the files under $OUTPUT_DIR/ for details."

# Exit code for CI
if [ "$unused_count" -gt 0 ] || [ "$duplicate_count" -gt 0 ]; then
  exit 2
else
  exit 0
fi
