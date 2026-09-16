# Defects

Status: `FIXED` = addressed and covered by an automated test; `OPEN` = not yet addressed.

1. Presets: user can't remove data from fields Sets, Reps, Weight and Rest — `FIXED` (`DraftExerciseRow` local text state; test `presetFields_canBeClearedAndRetyped`).
2. Default values for Barbell Squat is 3/10/20/90 — `FIXED` (`BuiltInExerciseCatalog.defaultPlanFor`; test `selectingExercise_prefillsSensibleDefaultsPerMovement`).
3. Routine execution: RPE field for Set 2 and Set 3 add weight to Set 1 until checked — `FIXED` (quick pad targets the last-edited set; test `routineQuickPad_actsOnLastEditedSet_notFirstUncompleted`).
4. There should be not only add weight button, reduce weight also — `FIXED` (`routine_weight_minus`; test `routineQuickPad_weightAndRepsAdjust`).
5. Not necessary to have kg and lb at the same time. Should automatically switch from metric to imperial depending on general app settings — `FIXED` (single unit system from app settings via `UnitSettings`; one pad step shown in the active unit).
6. After user checks checkbox on previous set, weight should be automatically copied to next Set. Same for reps — `FIXED` (`toggleSetCompleted` copies performed weight/reps to the next pristine set; tests `toggleSetCompleted_copiesPerformedWeightAndRepsToNextPristineSet` + `..._doesNotOverwriteUserEditedNextSet`).
7. There should be not only button to add weight, but to add/reduce reps number — `FIXED` (`routine_reps_plus` / `routine_reps_minus`; test `routineQuickPad_weightAndRepsAdjust`).
8. After user swaps exercise, Sets should reset to default values — `FIXED` (`swapRoutineExercise` resets sets to planned defaults; test `swapRoutineExercise_resetsSetsToPlannedDefaults`).
9. No haptic on Rest period ended — `FIXED` (`WorkoutHaptic.REST_ENDED`; test `restTimer_crossingZero_emitsRestEndedHaptic`).
10. If user unchecks checkbox, rest timer doesn't stop — `FIXED` (uncheck clears `restSeconds`; test `toggleSetCompleted_uncheckStopsRestTimer`).

11. User checks a checkbox, Rest timer started. If user swap to alternative excercise with 'Swap' button, rest timer still be present. — `FIXED` (`swapRoutineExercise` clears `restSeconds`; tests `swapRoutineExercise_stopsRestTimer` unit + `routineSwap_stopsRestTimer` UI).
12. Usual weight add for metric system should be devided on 1.25kg, not 2. If user want's to add 5kg it is easy - tap 1.25 4 times. — `FIXED` (`padStepWeightKg` metric step 1.25; tests `routineQuickPad_metricStepIsOnePointTwoFiveKg` + `routineQuickPad_weightAndRepsAdjust`).
13. On narrow screen the is no 'kg' or 'rep' visible on buttons. — `FIXED` (pad uses `FlowRow` so the four buttons wrap instead of truncating; labels keep `maxLines = 1`).
14. RPE field is redundant. Could be removed. — `FIXED` (RPE field removed from routine set rows and from `updateRoutineSet`; `StrengthSet.rpe` kept in the domain model for stored-session compatibility).
15. There is no General Settings to set the unit system — `FIXED` (`SettingsScreen` with metric/imperial selector via `UnitSettings`, entry from the History overflow menu, `settings` nav route).
16. While user do preset excercises it would be nice to have button to add additional set to excercise. — `FIXED` (`+ Set` button on each routine exercise card appends a planned set; tests `addRoutineSet_appendsPlannedSetAndIncrementsTargets` unit + `routineAddSet_appendsExtraPlannedSet` UI).
17. Instead haptic use vibration at the and of rest cycle and set — `FIXED` (system `Vibrator` with distinct `VibrationEffect` patterns for rest-end (200 ms) and set/exercise-complete (80 ms); `VIBRATE` permission added).
