# Custom-fork patches & CI

This branch (`custom`) is a fork of [MollyIM Android](https://github.com/mollyim/mollyim-android)
with a small set of "anti-censorship / retention" patches ported from the
[j0j1j2/Signal-Desktop](https://github.com/j0j1j2/Signal-Desktop) fork.

## Branch layout

- `main` — pure mirror of `mollyim/mollyim-android` (kept in sync automatically).
- `custom` — default branch. `main` + the private patches + custom CI.

## Patches

| Commit | Effect |
|--------|--------|
| build-expiry disable | The "outdated build" banner / confirmation / send-block are disabled (the build never "expires"). Remote server-driven deprecation is left intact. |
| retain view-once media | Received view-once (사진전용) photos/videos are **not** erased after you open them, so they can be opened again. |
| retain delete-for-everyone content | Received delete-for-everyone messages keep their body, quoted content, link previews, attachment files **and edit history** so they stay visible locally. Deleting a message *you sent*, or "delete for me", still erases it locally as usual. |

## How updates + builds work

- `.github/workflows/sync.yml` runs **daily at 03:00 UTC** (and manually):
  1. force-aligns `main` to the latest `mollyim/mollyim-android` `main`;
  2. rebases `custom` onto the fresh `main`. If an upstream change conflicts with
     a patch, the rebase is aborted and the run fails so it can be fixed.
  3. if new commits landed, builds via `release.yml`.
- `.github/workflows/release.yml` does the reproducible Docker build of `custom`,
  signs the APK when keystore secrets are configured, and updates a rolling
  `custom` GitHub release with the APK/AAB.

## Signing setup (optional, for a signed APK)

The workflow builds using the repo's Docker reproducible build and then signs the
resulting APKs with **uber-apk-signer**. Add these **Actions secrets** in
`Settings → Secrets and variables → Actions`:

| Secret | Value |
|--------|-------|
| `SECRET_KEYSTORE` | base64 of your `.jks` keystore (e.g. `base64 -w0 keystore.jks`) |
| `SECRET_KEYSTORE_ALIAS` | your key alias inside the keystore |
| `SECRET_KEYSTORE_PASSWORD` | keystore **and** key password (uber-apk-signer is called with the same password for both) |

If these are unset, the build produces an **unsigned** APK (fine for sideloading on
non-Play installs). Note that the **AAB is never signed** this way — uber-apk-signer
signs APKs only, so a Play-store AAB would need the app's native signing config.

You can instead download the built APK from the "Actions" run's **Artifacts**
or from the **`custom`** release page.

(Optional) to override the app name / package id, set repo **variables**
`CI_APP_TITLE`, `CI_APP_FILENAME`, `CI_PACKAGE_ID`, `CI_BUILD_VARIANTS`,
`CI_FORCE_INTERNAL_USER_FLAG`.
