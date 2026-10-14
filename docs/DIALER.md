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
- [~] 5. Test on emulator (`adb emu gsm call <number>` simulates an incoming call)

## Emulator test results (2026-10-14)
- ✅ `cmd role add-role-holder android.app.role.DIALER org.privacyrom.dialer 0` makes it the default.
- ✅ `adb emu gsm call 3331234567`: call screen shows only "Incoming call" + Answer/Decline/Reveal;
  heads-up notification says only "Incoming call".
- ✅ After Answer: shows nickname "Calm Falcon 15" and timer; Hang up works.
- ⬜ Not yet tested: Reveal, Decline, mute/speaker, dial pad + outgoing call, Recents list.

## Next steps
1. Bug: the heads-up notification still says "Incoming call" after answering. Re-post it on
   state change ("Call", no full-screen intent, `setOnlyAlertOnce`), or cancel the heads-up.
2. Test the remaining items above.
3. Optional "Reveal" protection with fingerprint/PIN (BiometricPrompt).
4. Contacts: show contact name only after Reveal (needs READ_CONTACTS).
