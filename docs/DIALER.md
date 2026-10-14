# Privacy Dialer — plan and progress

Written from scratch (like the keyboard) instead of forking: the LineageOS Dialer only builds
inside the full ROM tree, and Fossify Phone pulls in a large shared library. A minimal dialer
is small enough to audit. Android lets any app become the default phone app
(`RoleManager.ROLE_DIALER`), which gives it the in-call screen via `InCallService`.

## Features
1. **Nicknames**: every number shown as a stable generated nickname ("Quiet Otter"), derived
   from an HMAC of the number with a random per-device secret, so nicknames can't be matched
   across devices. Real number/contact shown only after an explicit "Reveal" tap.
2. **Incoming call**: full-screen screen shows "Incoming call" only, no name or number.
   Buttons: Answer, Decline, Reveal.
3. **In call**: nickname only; Reveal to see the number; Hang up, mute, speaker.
4. **Recents**: from the call log, showing nickname + direction + time; never the number.
5. **Dial pad**: digits typed are masked after a moment (like a password field).

## Steps (each pushed when done)
- [x] 1. Module skeleton + Nicknames generator + unit tests
- [x] 2. InCallService + in-call/incoming screen (answer/decline/hang up/reveal)
- [x] 3. (compiles, not yet tested on emulator) Default-dialer role request + dial pad (ACTION_DIAL / tel: handling)
- [x] 4. (compiles, not yet tested on emulator) Recents list from CallLog
- [x] 5. Test on emulator (`adb emu gsm call <number>` simulates an incoming call; `scripts/emu.sh install dialer` installs + makes default)

## Emulator test results (2026-10-14), screenshots in docs/screenshots/dialer-*.png
- ✅ Default dialer via `scripts/emu.sh install dialer`.
- ✅ Incoming: only "Incoming call"; heads-up says only "Incoming call".
- ✅ Reveal/Hide shows/hides the number; Answer shows nickname + timer; Decline; Hang up.
- ✅ After answering, the heads-up is replaced by a quiet low-priority "Call" notification (bug fixed).
- ✅ Mute. Speaker toggle can't be tested: the emulator is speaker-only.
- ✅ Dial pad masks digits; outgoing call shows nickname only.
- ✅ Recents: nickname + direction + time; same number = same nickname.

## Last changes, compiled but NOT yet tested on the emulator
- Reveal resets for each new call (call screen is reused between calls).
- Speaker toggle hidden when only one audio route exists.
- Recents says "just now" instead of "0 minutes ago".

## Next steps
1. Test the three changes above.
2. Optional "Reveal" protection with fingerprint/PIN (BiometricPrompt).
3. Contacts: show contact name only after Reveal (needs READ_CONTACTS).
