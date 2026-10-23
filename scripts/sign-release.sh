#!/bin/sh
set -eu

cd "$(dirname "$0")/.."
HAYATE_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$HAYATE_SDK" ] && [ -f local.properties ]; then
    HAYATE_SDK=$(sed -n 's/^sdk.dir=//p' local.properties)
fi
if [ -z "$HAYATE_SDK" ]; then
    echo 'Set ANDROID_HOME to the Android SDK directory.' >&2; exit 1
fi
HAYATE_BUILD_TOOLS="$HAYATE_SDK/build-tools/36.0.0"
if [ ! -f app/build/outputs/apk/release/app-release-unsigned.apk ]; then
    echo 'Run ./gradlew assembleRelease first.' >&2; exit 1
fi
umask 077
mkdir -p .signing dist
if [ ! -f .signing/hayate-release.p12 ]; then
    if [ -f .signing/password ]; then
        echo 'A signing password exists without its key; restore your signing backup.' >&2; exit 1
    fi
    python3 -c 'import secrets; from pathlib import Path; Path(".signing/password").write_text(secrets.token_urlsafe(48))'
    export HAYATE_SIGNING_PASSWORD="$(cat .signing/password)"
    keytool -genkeypair -keystore .signing/hayate-release.p12 -storetype PKCS12 \
        -alias hayate -keyalg RSA -keysize 3072 -validity 10000 \
        -storepass:env HAYATE_SIGNING_PASSWORD -keypass:env HAYATE_SIGNING_PASSWORD \
        -dname 'CN=Hayate Android, O=Hayate' >/dev/null
fi
export HAYATE_SIGNING_PASSWORD="$(cat .signing/password)"
"$HAYATE_BUILD_TOOLS/zipalign" -f -p 4 app/build/outputs/apk/release/app-release-unsigned.apk dist/hayate-aligned.apk
"$HAYATE_BUILD_TOOLS/apksigner" sign --ks .signing/hayate-release.p12 --ks-key-alias hayate \
    --ks-pass env:HAYATE_SIGNING_PASSWORD --key-pass env:HAYATE_SIGNING_PASSWORD \
    --out dist/hayate-0.2.0.apk dist/hayate-aligned.apk
unset HAYATE_SIGNING_PASSWORD
rm dist/hayate-aligned.apk
"$HAYATE_BUILD_TOOLS/apksigner" verify --verbose dist/hayate-0.2.0.apk
(cd dist && shasum -a 256 hayate-0.2.0.apk > SHA256SUMS)
echo 'Signed dist/hayate-0.2.0.apk. Keep .signing/ backed up securely for future updates.'
