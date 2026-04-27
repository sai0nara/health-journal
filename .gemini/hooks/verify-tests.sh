#!/usr/bin/env bash
# .gemini/hooks/verify-tests.sh

# Log file for debugging
LOG_FILE="/tmp/gemini_hook_debug.log"
echo "--- Hook Run at $(date) ---" >> $LOG_FILE

# Force Java 21 if available
if [ -d "/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home" ]; then
  export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home"
  echo "Using JAVA_HOME: $JAVA_HOME" >> $LOG_FILE
fi

# 1. Run Unit Tests (Always run)
echo "Running Unit Tests..." >> $LOG_FILE
./gradlew :app:test --quiet >> $LOG_FILE 2>&1
UNIT_EXIT=$?

if [ $UNIT_EXIT -ne 0 ]; then
  echo "Unit Tests Failed with code $UNIT_EXIT" >> $LOG_FILE
  cat <<EOF2
{
  "decision": "deny",
  "reason": "Unit tests failed. Check /tmp/gemini_hook_debug.log for details.",
  "systemMessage": "❌ Unit Tests Failed"
}
EOF2
  exit 0
fi

# 2. Skip UI Tests in automated hook to stay under 60s timeout
# UI tests should be run manually using the full command
echo "Skipping UI Tests in automated hook to ensure stability." >> $LOG_FILE

echo "Verification successful (Unit Tests passed)." >> $LOG_FILE
cat <<EOF4
{
  "decision": "allow",
  "systemMessage": "✅ Unit tests passed! (UI tests skipped for speed)"
}
EOF4
exit 0
