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