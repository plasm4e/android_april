# Roadmap

Base: **LineageOS 22 (Android 15)**. Work is split into *apps* (testable on any emulator now)
and *ROM changes* (need a full LineageOS build: ~400 GB disk, 32–64 GB RAM, so a cloud
VM such as Google Cloud, not this Codespace).

Status: ✅ done · 🚧 in progress · ⬜ planned

## Apps

| Feature | Where | Status |
|---|---|---|
| Whole-word input: apps' search/autocomplete only see complete words | `apps/keyboard` (WORD mode) | ✅ |
| Hidden typing: opaque curtain, app sees nothing until Send | `apps/keyboard` (HIDE mode) | ✅ |
| Go to home screen after a hidden send | `apps/keyboard` | ✅ (home only; force-closing the app needs ROM) |
| Dialer: no number/caller ID on incoming call or recents; generated nicknames, reveal on tap | fork of LineageOS Dialer or Fossify Phone | ⬜ |
| SMS: list shows nickname + content hint ("text", "photo", "code") + time | fork of Fossify Messages | ⬜ |
| File manager: no thumbnails, search-first instead of browsing | fork of Fossify File Manager | ⬜ |
| Maps: replace Google Maps | ship Organic Maps / CoMaps or OsmAnd | ⬜ |
| Browser | ship DuckDuckGo (open source) or Vanadium/Cromite; Opera is proprietary and cannot be redistributed | ⬜ |

## ROM changes (LineageOS source)

| Feature | Where in the source | Status |
|---|---|---|
| Status-bar notification icons replaced by a neutral dot; content hidden, title = app name or category | `frameworks/base/packages/SystemUI` | ⬜ |
| Recents: no screenshots/titles; auto-close tasks after screen-off or N minutes | `packages/apps/Launcher3` (Quickstep) + `TaskSnapshotController` | ⬜ |
| Sound off by default for ringtone/notifications (alarms and emergency alerts need care) | `frameworks/base` audio config overlays | ⬜ |
| New apps not added to the home screen | Launcher3 default setting | ⬜ |
| Hidden-keyboard helpers: force-close the app after send; press a chat app's own send button | privileged system service | ⬜ |
| System file picker without thumbnails | `packages/apps/DocumentsUI` | ⬜ |
| Bundle the apps above as system apps | `vendor/` makefiles | ⬜ |

Also review Android 15 **Private Space**: it already hides a locked profile's apps from
the launcher and recents.

## Known limits of the keyboard

- Chat apps often use a multi-line field with their own send button and no "Send" editor
  action. Hidden Send then only inserts the text; the user has to press SHOW APP and tap the
  app's button. A ROM-level helper can fix this.
- Word boundaries are letters/digits plus `' - _ @`; `.` ends a word, so an email address is
  delivered in chunks.
- English QWERTY only; no long-press accents yet.
