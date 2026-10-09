# Firebase Integration Prerequisites

Firebase is not wired into the current MVP yet. Do not add a real service-account key or private credentials to the repository.

When the Firebase project is ready:

1. Create or select a Firebase project.
2. Register Android app ID `com.urfavxbf.budgettracker`.
3. Download `google-services.json` into the local `app/` directory. It is excluded by `.gitignore`; do not commit it.
4. Add the Google Services Gradle plugin and Firebase BoM / Authentication / Cloud Firestore dependencies.
5. Implement authentication before any cloud reads or writes.
6. Add Firestore Security Rules that require authentication and restrict every document to the authenticated user's UID.
7. Test sign-in, sign-out, offline writes, reconnect sync, account isolation, and conflict behavior before enabling cloud sync.

Never ship permissive Firestore rules such as `allow read, write: if true`. Client-side filtering is not a substitute for server-enforced rules.
