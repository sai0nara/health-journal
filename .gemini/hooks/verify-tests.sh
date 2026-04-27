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

# Read hook input
input=$(cat)

# 1. Run Unit Tests
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

# 2. Check for Emulator
EMULATOR=$(adb devices | grep -w "device" | grep "emulator")

if [ -n "$EMULATOR" ]; then
  echo "Emulator detected. Running UI Tests..." >> $LOG_FILE
  
  # Clean old results on device to speed up pull
  adb shell rm -rf /sdcard/Download/allure-results/* >> $LOG_FILE 2>&1
  
  # Run UI Tests
  ./gradlew :app:connectedDebugAndroidTest --quiet >> $LOG_FILE 2>&1
  UI_EXIT=$?
  
  if [ $UI_EXIT -ne 0 ]; then
    echo "UI Tests Failed with code $UI_EXIT" >> $LOG_FILE
    cat <<EOF3
{
  "decision": "deny",
  "reason": "UI tests failed. Check /tmp/gemini_hook_debug.log for details.",
  "systemMessage": "❌ UI Tests Failed"
}
EOF3
    exit 0
  fi
  
  # Clean old report and Pull results
  rm -rf app/build/reports/allure-report/allureReport >> $LOG_FILE 2>&1
  ./gradlew :app:pullAllureResults :app:allureReport --quiet >> $LOG_FILE 2>&1
  echo "Allure report updated." >> $LOG_FILE
else
  echo "No emulator detected. Skipping UI tests." >> $LOG_FILE
fi

echo "All tests passed successfully!" >> $LOG_FILE
cat <<EOF4
{
  "decision": "allow",
  "systemMessage": "✅ All tests passed successfully!"
}
EOF4
exit 0
