# Theme Palette Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the DeepTeal admin theme with two new premium dark themes, then ship a GitHub release.

**Architecture:** Theme choices are represented by `AppThemeOption`, mapped to matte palettes in `Theme.kt`, surfaced in admin UI labels through `UiText.kt` and `AdminScreens.kt`, and covered by `ThemePaletteTest`. Release versioning lives in `app/build.gradle.kts`.

**Tech Stack:** Kotlin, Jetpack Compose, Gradle Android plugin, JUnit, GitHub CLI.

---

### Task 1: Theme Model And Tests

**Files:**
- Modify: `app/src/test/java/com/uzeyir/photoselector/ThemePaletteTest.kt`
- Modify: `app/src/main/java/com/uzeyir/photoselector/AdminSettings.kt`

- [ ] **Step 1: Write failing theme list expectation**

Change `selectableThemesAreOnlyTheThreeMatteDesignPalettes` to expect:

```kotlin
listOf(
    AppThemeOption.SignatureGold,
    AppThemeOption.RedBlackWhite,
    AppThemeOption.GraphiteCopper,
    AppThemeOption.MidnightRose
)
```

- [ ] **Step 2: Run focused test**

Run: `.\gradlew.bat testDebugUnitTest --tests com.uzeyir.photoselector.ThemePaletteTest`
Expected: FAIL because `GraphiteCopper` and `MidnightRose` are not defined.

- [ ] **Step 3: Replace enum**

In `AdminSettings.kt`, replace `DeepTeal` with:

```kotlin
GraphiteCopper,
MidnightRose
```

### Task 2: Palette And Labels

**Files:**
- Modify: `app/src/main/java/com/uzeyir/photoselector/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/uzeyir/photoselector/UiText.kt`
- Modify: `app/src/main/java/com/uzeyir/photoselector/AdminScreens.kt`
- Modify: `app/src/test/java/com/uzeyir/photoselector/AdminSettingsTest.kt`

- [ ] **Step 1: Add palette mappings**

Remove `DeepTealPalette`, add `GraphiteCopperPalette` and `MidnightRosePalette`, and map both in `paletteForOption`.

- [ ] **Step 2: Rename localized string fields**

Replace `themeDeepTeal` / `themeDeepTealDescription` with `themeGraphiteCopper`, `themeGraphiteCopperDescription`, `themeMidnightRose`, and `themeMidnightRoseDescription`.

- [ ] **Step 3: Update admin labels**

Map `AppThemeOption.GraphiteCopper` and `AppThemeOption.MidnightRose` to the new localized labels and descriptions.

- [ ] **Step 4: Update tests**

Use `AppThemeOption.GraphiteCopper` in admin settings persistence test where `DeepTeal` was used.

### Task 3: Version, Build, Release

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Bump version**

Increment `versionCode` and `versionName` by one patch version.

- [ ] **Step 2: Verify**

Run: `.\gradlew.bat testDebugUnitTest`
Run: `.\gradlew.bat assembleDebug`
Run the release build command used by the repository if available.

- [ ] **Step 3: Commit and release**

Run:

```bash
git add <changed files>
git commit -m "feat: refresh admin theme palettes"
git push origin main
gh release create <new-tag> <apk-asset> --repo uzeyireshref/PhotoSelector --title <new-tag> --notes <notes>
```
