# @path: hardcoded_drawables.sh

set -eu

MODULE_NAME="app"

RES_SEARCH_PATH="$MODULE_NAME/src/main/res"

BLACKLISTED_FILES="ic_launcher_background.xml ic_launcher_foreground.xml"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_DIR="build/hardcoded_color_analysis_${TIMESTAMP}"
REPORT_FILE="$OUTPUT_DIR/hardcoded_colors_report.txt"

VERBOSE=false

CI_CHECK=true

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

colored YELLOW "===== Starting Hardcoded Color Analysis in Drawables & Layouts ====="
log "Searching for drawable and layout XML files in: $RES_SEARCH_PATH"

find "$RES_SEARCH_PATH" -type f \
  \( -path "*/drawable*layout*drawable*layout*/*.xml" \) \
  -not -name "ic_launcher_background.xml" \
  -not -name "ic_launcher_foreground.xml" \
  | wc -l | tr -d ' ')
colored GREEN "  • Files Scanned:   $FILES_SCANNED"
colored RED   "  • Issues Found:    $TOTAL_FOUND"
colored GREEN "  • Report file:     $REPORT_FILE"

if [ "$CI_CHECK" = true ] && [ "$TOTAL_FOUND" -gt 0 ]; then
  colored RED "\n❌ CI-check FAILED: Hardcoded colors were found in drawable or layout resources."
  exit 2
fi

colored GREEN "\n✅ Analysis finished successfully."
exit 0
