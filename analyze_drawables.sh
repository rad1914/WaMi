# @path: analyze_drawables.sh

set -eu

MODULE_NAME="app"
RES_SEARCH_PATH="$MODULE_NAME/src/main/res"
CODE_DIRS="$MODULE_NAME/src/main"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_DIR="build/res_analysis_${TIMESTAMP}"
AGG_OUT="$OUTPUT_DIR/all_unused_files.txt"
LOG_FILE="$OUTPUT_DIR/actions.log"
BACKUP_DIR="$OUTPUT_DIR/backups"

DRY_RUN=false
DELETE_UNUSED=true
FORCE_DELETE=true
VERBOSE=false
CI_CHECK=true

TYPES="drawable,mipmap,layout,color,string"

usage() {
  cat <<EOF
Usage: $0 [options]

Analyzes Android project resources and optionally deletes unused ones.

Options:
  -n, --dry-run        Don’t actually delete; just report.
  -d, --delete         Enable deletion of detected unused resources.
  -f, --force          Don’t prompt before deleting each file.
  -v, --verbose        Show debug logging.
  -c, --ci-check       Exit non-zero if any unused resources are found.
  -t, --types=LIST     Comma-separated list of types to analyze (default: $TYPES).
  -h, --help           Show this help and exit.
EOF
  exit 1
}

get_usage_regex() {
    case "$1" in
      drawable) echo "(@drawable/|R\.drawable\.|tools:src=|tools:background=|@{.*drawable/)" ;;
      mipmap)   echo "(@mipmap/|R\.mipmap\.|@{.*mipmap/)" ;;
      layout)   echo "(@layout/|R\.layout\.|tools:layout=|@{.*layout/)" ;;
      color)    echo "(@color/|R\.color\.|@{.*color/)" ;;
      string)   echo "(@string/|R\.string\.|@{.*string/)" ;;
      *)        return 1 ;;
    esac
}

log() {
  if [ "$VERBOSE" = "true" ]; then
    printf "[LOG] %s\n" "$*" >&2
  fi
}

colored() {
  local code
  case "$1" in
    RED)    code=31 ;;
    GREEN)  code=32 ;;
    YELLOW) code=33 ;;
    *)      code=0 ;;
  esac
  shift
  printf "\033[1;%sm%s\033[0m\n" "$code" "$*"
}

confirm_delete() {
  [ "$FORCE_DELETE" = true ] && return 0
  printf "Delete '%s'? [y/N]: " "$1"
  local resp
  read -r resp
  case "$resp" in
    [yY]|[yY][eE][sS]) return 0 ;;
    *) return 1 ;;
  esac
}

while [ $# -gt 0 ]; do
  case "$1" in
    -n|--dry-run)    DRY_RUN=true; shift ;;
    -d|--delete)     DELETE_UNUSED=true; shift ;;
    -f|--force)      FORCE_DELETE=true; shift ;;
    -v|--verbose)    VERBOSE=true; shift ;;
    -c|--ci-check)   CI_CHECK=true; shift ;;
    -t|--types)
      if [ -n "$2" ]; then
        TYPES="$2"
        shift 2
      else
        colored RED "ERROR: Option '$1' requires an argument."
        exit 1
      fi
      ;;
    --types=*)
      TYPES="${1#*=}"
      shift 1
      ;;
    -h|--help)       usage ;;
    --)              shift; break ;;
    -*)
      colored RED "ERROR: Unknown option '$1'"
      usage
      ;;
    *) break ;; # End of options
  esac
done

if [ ! -d "$RES_SEARCH_PATH" ]; then
  colored RED "❌ Resource directory not found: $RES_SEARCH_PATH"
  echo "Hint: Run this from the project root and ensure MODULE_NAME is correct." >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"
mkdir -p "$BACKUP_DIR"
: > "$AGG_OUT"
: > "$LOG_FILE"

analyze_type() {
  local type="$1"
  local regex; regex=$(get_usage_regex "$type")
  if [ -z "$regex" ]; then
    log "Warning: Unknown resource type '$type'. Skipping."
    return
  fi
  local dir_glob="${type}*"
  local unused_file="$OUTPUT_DIR/unused_${type}s.txt"
  local dup_file="$OUTPUT_DIR/duplicate_${type}s.txt"
  local tmp_hash="$OUTPUT_DIR/hashes_${type}s.txt"

  : > "$unused_file"
  : > "$dup_file"

  colored YELLOW "🔎 Analyzing '$type' resources…"

  find "$RES_SEARCH_PATH" -type d -name "$dir_glob" -print0 \
    | xargs -0 -I{} find "{}" -type f -print0 \
    > "$OUTPUT_DIR/all_${type}_files0.txt"

  tr '\0' '\n' < "$OUTPUT_DIR/all_${type}_files0.txt" \
    | xargs -n1 basename \
    | sed -E 's/(\.9)?\.[^.]+$
    | sort -u \
    > "$OUTPUT_DIR/basenames_${type}s.txt"

  local file_count
  file_count=$(wc -l < "$OUTPUT_DIR/basenames_${type}s.txt" | tr -d ' ')
  log "Found $file_count unique '$type' basenames."

  local master_regex
  master_regex=$(tr '\n' '|' < "$OUTPUT_DIR/basenames_${type}s.txt" | sed 's/|$

  if [ -z "$master_regex" ]; then
    log "No basenames found for type '$type'. Skipping."
  else
    master_regex="(${master_regex})"
    log "Searching for usages with master regex..."
    grep -R --include='*.{java,kt,xml}' -E -o "${regex}${master_regex}\b" "$CODE_DIRS" \
      | sed -E "s/.*(${regex})
      | sort -u \
      > "$OUTPUT_DIR/used_${type}s.txt"
    log "Finished single-pass search. Identified used resources."

    comm -23 "$OUTPUT_DIR/basenames_${type}s.txt" "$OUTPUT_DIR/used_${type}s.txt" \
      | while IFS= read -r base; do
          log "Unused $type basename: $base"
          grep "/${base}\.[^/]*$" "$OUTPUT_DIR/all_${type}_files0.txt" | tr -d '\0' \
            >> "$unused_file"
        done
  fi

  tr '\n' '\0' < "$unused_file" > "${unused_file}.0"
  mv "${unused_file}.0" "$unused_file"

  local unused_count
  unused_count=$(tr -cd '\0' < "$unused_file" | wc -c)
  colored GREEN "✓ Found unused $type(s): $unused_count entries"

  local hash_cmd="md5sum"
  command -v md5sum >/dev/null || hash_cmd="md5 -r"
  log "Using hash command: $hash_cmd"

  tr '\0' '\n' < "$OUTPUT_DIR/all_${type}_files0.txt" \
    | xargs -I{} $hash_cmd "{}" \
    | awk '{print $1, $2}' \
    > "$tmp_hash"

  sort "$tmp_hash" | awk -v out="$dup_file" '
    {
      h=$1; f=$2;
      if (h!=prev && NR>1) {
        if (cnt>1) {
          printf "Group (hash: %s):\n", prev >> out
          for (i=1;i<=cnt;i++) printf "  - %s\n", fls[i] >> out
          printf "\n" >> out
        }
        cnt=0
        delete fls
      }
      cnt++; fls[cnt]=f; prev=h
    }
    END {
      if (cnt>1) {
        printf "Group (hash: %s):\n", prev >> out
        for (i=1;i<=cnt;i++) printf "  - %s\n", fls[i] >> out
        printf "\n" >> out
      }
    }'

  rm -f "$tmp_hash"
  if [ -s "$dup_file" ]; then
    colored GREEN "✓ Duplicate '$type' groups found → $dup_file"
  else
    log "No duplicate '$type' files found."
    rm -f "$dup_file"
  fi

  cat "$unused_file" >> "$AGG_OUT"
}

colored YELLOW "===== Starting Resource Analysis ====="
log "Output will be in: $OUTPUT_DIR"

OLD_IFS=$IFS
IFS=','
set -- $TYPES
for type in "$@"; do
  type_trimmed=$(echo "$type" | tr -d '[:space:]')
  if [ -n "$type_trimmed" ]; then
    analyze_type "$type_trimmed"
  fi
done
IFS=$OLD_IFS

total_unused=$(tr -cd '\0' < "$AGG_OUT" | wc -c)
colored YELLOW "\nSummary:"
colored GREEN "  • Unused resources: $total_unused"
colored GREEN "  • Delete mode:      $DELETE_UNUSED"
colored GREEN "  • Dry-run:          $DRY_RUN"
colored GREEN "  • CI-check:         $CI_CHECK"

if [ "$DELETE_UNUSED" = true ]; then
  if [ "$total_unused" -eq 0 ]; then
    colored GREEN "✨ No unused resources to delete."
  else
    colored RED "⚠️  $total_unused unused files targeted for deletion."
    log "Backups of deleted files will be stored in: $BACKUP_DIR"

    tr '\0' '\n' < "$AGG_OUT" | while read -r file; do
      case "$file" in
        "$RES_SEARCH_PATH"/*) ;;
        *) log "SKIP suspicious path: $file"; continue ;;
      esac

      if [ ! -f "$file" ]; then
        log "File not found (might have been already deleted): $file"
        continue
      fi

      if confirm_delete "$file"; then
        if [ "$DRY_RUN" = false ]; then
          mv "$file" "$BACKUP_DIR/"
          printf "Deleted: %s (backed up)\n" "$file" >> "$LOG_FILE"
          colored GREEN "Deleted: $file"
        else
          colored YELLOW "(dry-run) Would delete: $file"
        fi
      else
        log "Skipped by user: $file"
      fi
    done
    [ "$DRY_RUN" = false ] && colored GREEN "\n✓ Deletion complete. Backups are in $BACKUP_DIR"
  fi
fi

if [ "$CI_CHECK" = true ] && [ "$total_unused" -gt 0 ]; then
  colored RED "\n❌ CI-check FAILED: $total_unused unused resources found."
  exit 2
fi

colored GREEN "\n✅ Analysis successful. Reports are in: $OUTPUT_DIR"
exit 0
