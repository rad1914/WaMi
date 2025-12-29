#!/usr/bin/env sh
set -eu

### ─── Configuration ──────────────────────────────────────────────────────────

# The Android module to analyze (e.g., "app", "library")
MODULE_NAME="app"
# The base path for the module's resource directory
RES_SEARCH_PATH="$MODULE_NAME/src/main/res"

# --- Blacklist Configuration
# Files listed here will be ignored by the analysis. This is hardcoded
# into the find command below for maximum portability and simplicity.
# By default, we ignore standard launcher icon files which often use hex colors.
BLACKLISTED_FILES="ic_launcher_background.xml ic_launcher_foreground.xml"

# --- Output Configuration
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_DIR="build/hardcoded_color_analysis_${TIMESTAMP}"
REPORT_FILE="$OUTPUT_DIR/hardcoded_colors_report.txt"

# --- Script Behavior Defaults
VERBOSE=false
# By default, the script will exit with a non-zero code if issues are found.
CI_CHECK=true

### ─── Helpers ────────────────────────────────────────────────────────────────

usage() {
  cat <<EOF
Usage: $0 [options]

Analyzes Android drawable and layout XML files for hardcoded hex colors.

This script searches for color values like '#FFFFFF' or '#FF000000'
directly within drawable and layout XML files. Using color resources
(e.g., @color/white) is the recommended practice.

It automatically ignores common generated files: $BLACKLISTED_FILES

Options:
  -v, --verbose        Show debug logging.
  -c, --ci-check       Exit non-zero if any hardcoded colors are found (default: true).
  --no-ci-check      Disable non-zero exit code on finding issues.
  -h, --help           Show this help and exit.
EOF
  exit 1
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

### ─── Parse Options (Portable while-loop) ──────────────────────────────────

while [ $# -gt 0 ]; do
  case "$1" in
    -v|--verbose)    VERBOSE=true; shift ;;
    -c|--ci-check)   CI_CHECK=true; shift ;;
    --no-ci-check)   CI_CHECK=false; shift ;;
    -h|--help)       usage ;;
    --)              shift; break ;;
    -*)
      colored RED "ERROR: Unknown option '$1'"
      usage
      ;;
    *) break ;; # End of options
  esac
done

### ─── Pre-Flight Checks ──────────────────────────────────────────────────────

if [ ! -d "$RES_SEARCH_PATH" ]; then
  colored RED "❌ Resource directory not found: $RES_SEARCH_PATH"
  echo "Hint: Run this from the project root and ensure MODULE_NAME ('$MODULE_NAME') is correct." >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"
: > "$REPORT_FILE"

log "Output directory created at: $OUTPUT_DIR"
log "CI Check mode: $CI_CHECK"
log "Ignoring files: $BLACKLISTED_FILES"

### ─── Core Analysis ─────────────────────────────────────────────────────────

colored YELLOW "===== Starting Hardcoded Color Analysis in Drawables & Layouts ====="
log "Searching for drawable and layout XML files in: $RES_SEARCH_PATH"

# The grep command searches for attributes containing a hex color.
# The pattern looks for a quote, a hash, and then 6 or 8 hex characters.
# We search in both 'drawable*' and 'layout*' directories, excluding blacklisted files.
# `|| true` prevents the script from exiting if grep finds no matches (due to `set -e`).
find "$RES_SEARCH_PATH" -type f \
  \( -path "*/drawable*/*.xml" -o -path "*/layout*/*.xml" \) \
  -not -name "ic_launcher_background.xml" \
  -not -name "ic_launcher_foreground.xml" \
  -print0 \
  | xargs -0 grep -E -H -n --color=never '["'"'"'](#[0-9a-fA-F]{6}|#[0-9a-fA-F]{8})["'"'"']' \
  > "$REPORT_FILE" || true


# Count the number of findings
TOTAL_FOUND=$(wc -l < "$REPORT_FILE" | tr -d ' ')

### ─── Report Summary ────────────────────────────────────────────────────────

if [ "$TOTAL_FOUND" -gt 0 ]; then
  colored RED "\n❌ Found $TOTAL_FOUND hardcoded color(s) in the following files:"
  
  # Pretty print the report with highlighted matches
  while IFS= read -r line; do
    # Extract file path and the rest of the line
    FILE_PATH=$(echo "$line" | cut -d: -f1)
    LINE_CONTENT=$(echo "$line" | cut -d: -f2-)
    
    # Print file path in yellow
    printf "\n"
    colored YELLOW "File: $FILE_PATH"
    
    # Print the line content and highlight the hex code in red
    echo "$LINE_CONTENT" | sed -E "s/(#[0-9a-fA-F]{6,8})/$(printf '\033[1;31m%s\033[0m' '\1')/g"
  done < "$REPORT_FILE"
  
  printf "\n"
  colored YELLOW "Recommendation: Replace hardcoded colors with references to 'res/values/colors.xml'."

else
  colored GREEN "\n✅ Analysis complete. No hardcoded colors found in drawable or layout XML files."
fi

colored YELLOW "\n===== Analysis Summary ====="
# Count all scanned files for the summary report, excluding the blacklist
FILES_SCANNED=$(find "$RES_SEARCH_PATH" -type f \
  \( -path "*/drawable*/*.xml" -o -path "*/layout*/*.xml" \) \
  -not -name "ic_launcher_background.xml" \
  -not -name "ic_launcher_foreground.xml" \
  | wc -l | tr -d ' ')
colored GREEN "  • Files Scanned:   $FILES_SCANNED"
colored RED   "  • Issues Found:    $TOTAL_FOUND"
colored GREEN "  • Report file:     $REPORT_FILE"


### ─── CI-Check Exit ─────────────────────────────────────────────────────────

if [ "$CI_CHECK" = true ] && [ "$TOTAL_FOUND" -gt 0 ]; then
  colored RED "\n❌ CI-check FAILED: Hardcoded colors were found in drawable or layout resources."
  exit 2
fi

colored GREEN "\n✅ Analysis finished successfully."
exit 0
