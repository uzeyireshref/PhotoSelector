#   # PhotoSelector - Project Goal

PhotoSelector is an Android tablet app for photographers.

The app allows a photographer to show JPG photos from an SD card to a customer in a fast, clean, premium-looking gallery interface.

The customer can:
- Browse all JPG photos in a gallery
- Open a photo in full screen
- Zoom into photos
- Like/unlike photos
- See the selected photo count
- See live price updates
- See automatic discounts after a certain number of liked photos
- Continue to a review screen that shows only liked photos
- Remove likes in the review screen
- Quickly return from the review screen to the full gallery
- Finalize the selection

After final confirmation, the app copies:
- The selected JPG files
- The matching RAW files with the same base filename

into a new folder on the same SD card.

Example:
IMG_0001.JPG selected
Matching RAW could be:
IMG_0001.CR3
IMG_0001.CR2
IMG_0001.NEF
IMG_0001.ARW

The first version focuses only on the customer-facing gallery experience.
Admin panel will be added later.

## Language and Pricing Direction

First version UI language:
- Turkish

Future language support:
- Turkish / English language selection will be added later.
- User-facing text should be kept centralized where possible so runtime language selection can be added without rewriting screens.

Current pricing:
- One selected photo costs 300 TL.
- The real price is selected photo count multiplied by 300 TL.
- No discount is applied for 1, 2, or 3 selected photos.
- Discount starts at 4 selected photos and increases by tier until 10 selected photos.
- Discount table:
  - 4 photos: 5%
  - 5 photos: 10%
  - 6 photos: 15%
  - 7 photos: 20%
  - 8 photos: 25%
  - 9 photos: 30%
  - 10 photos: 35%
- The payable discounted price is capped after 10 selected photos. With the current table, 10 or more selected photos cost 1950 TL.
- When a discount applies, the real price should still be shown with a strikethrough and the discounted payable price should remain visible.

Future admin panel:
- Photo price, price cap/discount limit, discount tier table, and language settings should be editable from the admin panel.

## Technical Direction

Platform:
- Android tablet
- APK installation, no Google Play requirement

Technology:
- Kotlin
- Jetpack Compose
- Storage Access Framework
- DocumentFile

Architecture rule:
UI code and business logic must be separated.

Core logic should stay separate from UI because a Windows version may be created later.

Main modules:
- UI screens
- Photo scanning
- RAW matching
- Selection management
- Price calculation
- File export/copying

First MVP:
- Folder select screen
- JPG gallery screen
- Fullscreen photo viewer
- Like/unlike
- Live bottom price bar
- Liked photos review screen
- Final confirmation
- Copy liked JPG + matching RAW files into a new folderProject Goals
