# Releasing a new version

1. In `android-app/app/build.gradle` raise `versionCode` (+1) and set `versionName`.
2. Add a section to `CHANGELOG.md`.
3. Commit and push to `main`.
4. Tag the release and push the tag:

   ```bash
   git tag v1.2.0
   git push origin v1.2.0
   ```

5. GitHub Actions builds both downloads and attaches them to a new Release called `v1.2.0`:
   the APK (`build-apk.yml`) and the Windows installer (`build-windows-server.yml`).
   Check the **Actions** tab if they do not appear. The installer version is taken from the tag.

The APK from Actions is a *debug* build: it installs fine on any phone (people must allow
"install unknown apps"). For a properly signed release build, create a keystore in Android Studio
(Build > Generate Signed App Bundle / APK), keep it and its passwords safe, and never commit it.
