# Hayate tools implementation plan

> **For agentic workers:** Use superpowers:executing-plans to implement task by task.

**Goal:** Ship Hayate 0.2.0 with offline suggestions, clipboard, programmer tools and original-designer credits.

**Architecture:** JSymSpell supplies correction candidates behind an asynchronous editor-context adapter. A shared KeyboardSession connects the existing keyboard to the IME and practice field; platform ClipboardManager supplies clips.

**Tech stack:** Java 17, Android API 26–36, JSymSpell 1.0, JUnit 4, framework device tests.

**Spec:** `docs/superpowers/specs/2026-10-24-hayate-tools-design.md`

## Global constraints

- Offline; no INTERNET permission, typed-text persistence, or automatic correction.
- Keep original letter gesture positions, appearance and navigation insets.
- Optional recent clips: ten maximum, memory only, one-hour expiry, sensitive clips excluded.
- Suggestions and clipboard disabled in password fields; suggestions respect NO_SUGGESTIONS and code mode.

## Review focus

- Candidate from an earlier field/cursor position must not replace new text.
- Editor selection and emoji offsets must remain intact after code pairing.
- Clipboard listeners must stop when the keyboard is hidden.
- Layer switches and asynchronous results must not restore hidden/password suggestions.
- New rows must fit both orientations and preserve old gesture tests.

## Tasks

- [x] Write failing unit tests for suggestion ranking/case, word context and bounded clipboard memory. Add the pinned engine, licensed dictionary and minimal adapters; rerun the unit suite.
- [x] Write device tests for candidate replacement, clipboard paste/sensitive clips and programmer pairing/indentation. Implement shared session, accessory row, clipboard panel, code layout and settings; run device suite.
- [x] Add original-designer credit and bundled third-party notices, update privacy/release documentation and version/signing paths.
- [x] Run full verification, review the changes, fix material findings, capture screenshots and sign the new APK.

## Execution record

Native implementation on a feature branch in the existing checkout; avoids creating another checkout of the user's small dedicated project. Defaults above are builder decisions under the user's implementation request.

Review regressions: selected-text retrieval failure now recovers the extracted selection; confirmed RED () then GREEN (name). The API 26 clipboard test cleanup uses the platform fallback. Clipboard memory now expires on a timer even when shelves are hidden. Leaving code mode restores editor capitalization (RED e, GREEN E). Manual external-app return exposed a stale IME window; the live service now requests hide when practice resumes/focuses. Verified Settings mInputShown=true → Hayate mInputShown=false.

Final evidence: 21 JVM tests + 21 instrumented tests passed, lint 0 errors / 15 warnings. Final signed APK 291,711 bytes; SHA-256 00ec871b99312558658a8a089e7c6ba4c414725b5840ea53b896b2aa4c73bfe7. Release publication follows the verified source commit.
