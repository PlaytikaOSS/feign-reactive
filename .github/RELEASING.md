# Maven Central releases

This repository calls the shared
[PlaytikaOSS release workflow](https://github.com/PlaytikaOSS/playtika-workflows)
at published commit `66a9a97773cc85395300f855935abc0e169a615b`. The two workflow
SHA pins must remain identical when upgrading.

The shared workflow validates approved actors and an immutable release tag on
protected `develop`, builds without publication secrets, attests the bundle, then
uses the official JReleaser action to sign and publish after Environment approval.
Manual publication and the GitHub App write token are removed.

Follow the shared repository's README to configure `RELEASE_ACTORS`, protected
version tags, and the `maven-central` Environment with independent reviewers and
its five signing/publication secrets. The build uses JDK 21. Test execution is
handled by the required Java CI workflow; the release build stages artifacts
with `skip-tests: true`.

Release policy and regression tests live in the shared repository so the same
implementation can be maintained and rolled out across PlaytikaOSS.
