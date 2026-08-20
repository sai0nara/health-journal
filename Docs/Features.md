
[//]: # (10. User should be able to add more than one attachment: common formats. )
[//]: # (And more than one photo. User should be able to save attachments synchronized from the cloud.)
[//]: # (2. add photos)
[//]: # (4. search)
[//]: # (7. allure report with screenshots)
[//]: # (8. add versions to app-debug.apk name and on screen &#40;about app&#41;)
<!-- * Ability to move entry to archive. From archive user can delete any entry, couple or all of them  -->
* Add different categories/tags: illness, checkup, doctor, exercises. Search by these categories/tags
* calendar with events from one day or period 
* graph, trends
* Body parts measurements 
* change color scheme to more common for medical apps. App should have an ability to get theme settings from the System. Add dark theme. UI defect: user can't see status bar since it is white
<!-- * For a text larger than 3 strings should be displayed only first 3 strings. Basic formatting tools, like bold, italic, headers -->
<!-- * User should be able to save attachments in any directory he choise -->
* Defect: Attachment icon on toolbar opens camera instead of gallery
* Defect: pull the feed down triggers syncronization but the update icon newer dissaper  

1. Medical App Color System (Light & Dark)To support dynamic system themes, we must stop using absolute colors (e.g., "Blue") and start using Semantic Tokens (e.g., "Primary", "Background"). For Dark Mode, we don't just invert colors; we use dark elevated grays to reduce eye strain and slightly desaturate our primary colors so they don't visually "vibrate" against dark backgrounds.Semantic TokenLight Theme (Medical Standard)Dark Theme (Eye-strain Reduction)Usage ContextBackground#F8F9FA (Off-white/Clinical)#121212 (Deep Charcoal)Main app background, behind cards.Surface#FFFFFF (Pure White)#1E1E1E (Elevated Gray)Cards, modals, archive list items.Primary#0A66C2 (Trust/Health Blue)#4A90E2 (Desaturated Blue)Key actions, active states, checkboxes.Secondary#20C997 (Success/Healing Teal)#48D8A4 (Desaturated Teal)Favorable actions, "Undo" toasts.Text Primary#212529 (High-contrast Dark)#E9ECEF (Soft White)Main entry titles, reading text.Text Secondary#6C757D (Muted Gray)#A0AAB2 (Lighter Gray)Timestamps, archived dates.Error/Destructive#DC3545 (Standard Red)#EF5350 (Softer Red)"Delete All" buttons, hard warnings.2. System Theme Integration ArchitectureThe app needs to default to the user's OS preference, but allow them to override it if desired.The Data Flow:App launches -> Checks local storage for user override (e.g., ThemeMode.LIGHT or ThemeMode.DARK).If no override exists, fallback to ThemeMode.SYSTEM.The app subscribes to OS configuration changes. If the user toggles Dark Mode in their Control Center / Quick Settings, the app UI re-renders instantly.Implementation by Platform:iOS (SwiftUI): System theming is handled automatically if you use standard color assets in your Assets.xcassets catalog (where you define "Any Appearance" and "Dark Appearance" for each color). You can override it locally using the .preferredColorScheme(.dark) modifier on your root view.Android (Jetpack Compose): Use isSystemInDarkTheme() to detect the OS state. Pass this boolean into your custom MedicalAppTheme composable, which maps to either your LightColors or DarkColors palette.Flutter: In your MaterialApp, set theme: lightTheme, darkTheme: darkTheme, and themeMode: ThemeMode.system.3. Fixing the UI Defect: The Invisible Status BarThe issue here is that the OS assumes your app has a dark header, so it paints the status bar text/icons white. Because your app background is also white, it disappears. We need to tell the OS that your app has a "Light" background, forcing the status bar icons to become dark.How to fix it:iOS (Swift/UIKit/SwiftUI):If your UIWindow or main view has a white background, the OS usually adapts. If it's stuck, force it in your Info.plist or view.SwiftUI: Ensure your top view ignores safe areas properly or use .toolbarColorScheme(.light, for: .navigationBar).UIKit: Override preferredStatusBarStyle in your View Controller to return .darkContent (which means dark text/icons for a light background).Android (Kotlin/Jetpack Compose):You need to adjust the System UI Controller. Since API 30, this is done via Window Insets.In Compose (using accompanist or modern EdgeToEdge):Kotlinval systemUiController = rememberSystemUiController()
val useDarkIcons = !isSystemInDarkTheme() // Dark icons in Light Mode

SideEffect {
    systemUiController.setStatusBarColor(
        color = Color.Transparent,
        darkIcons = useDarkIcons
    )
}
In XML/Themes (themes.xml):Add <item name="android:windowLightStatusBar">true</item> to your Light theme (forces dark icons). Set it to false in your Dark theme.Flutter:Use the AnnotatedRegion widget or adjust the AppBar system overlay style:DartAppBar(
  systemOverlayStyle: SystemUiOverlayStyle(
    statusBarColor: Colors.transparent, // Match your background
    statusBarIconBrightness: Brightness.dark, // Dark icons for Light theme
    statusBarBrightness: Brightness.light, // For iOS
  ),
)