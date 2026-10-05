# Keep rules for the `dist` build type (minified distribution APK).
# Room, CameraX, Coil, OkHttp, Health Connect and Compose ship their own
# consumer rules; only reflection-heavy libs without complete rules need
# explicit keeps below.

-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault,EnclosingMethod,InnerClasses

# Google API client + Drive service: GenericData models are (de)serialized
# via @Key reflection; stripping them breaks sync/backup payloads.
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.auth.** { *; }

# iText7 (PDF export): font/resource lookup uses reflection.
-keep class com.itextpdf.** { *; }

# Room: entities and database implementations referenced via generated code.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Gson: models (de)serialized via TypeToken reflection in converters, sync
# payloads and backup codecs. Without explicit keeps, R8 renames fields and
# breaks round-trips (reads silently degrade to empty via catch-all fallbacks).
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.example.healthjournal.domain.PresetExercise { *; }
-keep class com.example.healthjournal.domain.StrengthExercise { *; }
-keep class com.example.healthjournal.domain.WorkoutIntervalSession { *; }
-keep class com.example.healthjournal.data.local.JournalEntry { *; }
-keep class com.example.healthjournal.data.local.DeletedEntry { *; }
-keep class com.example.healthjournal.data.local.EntryTagCrossRef { *; }
-keep class com.example.healthjournal.data.local.BodyMeasurementEntry { *; }
-keep class com.example.healthjournal.data.local.GoalEntity { *; }
-keep class com.example.healthjournal.data.local.PersonalCard { *; }
-keep class com.example.healthjournal.data.local.WorkoutSession { *; }
-keep class com.example.healthjournal.data.local.WorkoutPreset { *; }
-keep class com.example.healthjournal.data.local.ExerciseCatalogItem { *; }
-keep class com.example.healthjournal.data.local.AttachmentData { *; }

# Desktop-only transitive references never loaded on Android (AWT image
# paths, Jackson fallback in iText-commons, Kerberos in Apache HTTP,
# slf4j binder): silence, do not keep.
-dontwarn com.fasterxml.jackson.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.apache.http.**
