# First time on GitHub

1. Create an empty repository on github.com (no README, no .gitignore, no licence: they already exist here).
2. In this folder run:

   ```bash
   git init -b main
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/<your-user>/<your-repo>.git
   git push -u origin main
   ```

3. Open the **Actions** tab and wait for "Build APK" to turn green.
4. Upload `release-assets/PC-Remote-v1.1.apk` (or tag `v1.1.0`, see `RELEASING.md`) so people have a download.
5. Add your own screenshots of the new screens to `docs/screenshots/` and link them in the README.

Before making the repository public, read the Security section of the README.
