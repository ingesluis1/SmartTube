#!/bin/bash
# Publiceert een nieuwe versie van SmartTube (Inge) als update.
#
# Gebruik (Git Bash, in de projectmap):
#   tools/publish_update.sh "Wat er nieuw is" ["Nog iets nieuws" ...]
#
# Wat het doet:
#   1. versionCode +1 (en versionName laatste cijfer +1) in smarttubetv/build.gradle
#   2. release bouwen (ondertekend met de eigen sleutel)
#   3. apk's + stable.json klaarzetten in build/update-site/smarttube/
#   4. publiceren als GitHub Release in ingesluis1/SmartTube-Inge (gh CLI)
#   5. deelmap Downloads/SmartTube_Inge bijwerken
#
# De app kijkt bij elke start naar:
#   https://github.com/ingesluis1/SmartTube-Inge/releases/latest/download/stable.json
#
# Opties via omgevingsvariabelen:
#   NO_BUMP=1        versienummer niet ophogen
#   NO_UPLOAD=1      alleen lokaal klaarzetten, niet publiceren
#   NO_SHARE=1       deelmap Downloads/SmartTube_Inge niet bijwerken
#   BASE_URL=...     downloadadres in stable.json (standaard de GitHub Release van deze versie)
#   REPO=...         GitHub-repository (standaard ingesluis1/SmartTube-Inge)
set -e
cd "$(dirname "$0")/.."

if [ $# -lt 1 ]; then
    echo "Geef minstens één regel 'wat is er nieuw' mee, bv: tools/publish_update.sh \"Nieuw zoekscherm\""
    exit 1
fi

GRADLE=smarttubetv/build.gradle
REPO="${REPO:-ingesluis1/SmartTube-Inge}"

if [ -z "$NO_BUMP" ]; then
    python -I - "$GRADLE" <<'EOF'
import io, re, sys
p = sys.argv[1]
s = io.open(p, encoding="utf-8").read()
code = int(re.search(r"versionCode (\d+)", s).group(1)) + 1
name = re.search(r'versionName "([^"]+)"', s).group(1)
parts = name.split(".")
parts[-1] = str(int(parts[-1]) + 1) if parts[-1].isdigit() else parts[-1] + ".1"
name2 = ".".join(parts)
s = re.sub(r"versionCode \d+", "versionCode %d" % code, s, count=1)
s = re.sub(r'versionName "[^"]+"', 'versionName "%s"' % name2, s, count=1)
io.open(p, "w", encoding="utf-8", newline="\n").write(s)
print("Nieuwe versie: %s (%d)" % (name2, code))
EOF
fi

VERSION_CODE=$(python -I -c "import re,sys;print(re.search(r'versionCode (\d+)',open(sys.argv[1]).read()).group(1))" "$GRADLE")
VERSION_NAME=$(python -I -c "import re,sys;print(re.search(r'versionName \"([^\"]+)\"',open(sys.argv[1]).read()).group(1))" "$GRADLE")
TAG="v${VERSION_NAME}"
BASE_URL="${BASE_URL:-https://github.com/${REPO}/releases/download/${TAG}}"

export JAVA_HOME="${JAVA_HOME:-C:/Users/Gebruiker/AndroidStudioProjects/tools/jdk-17.0.20.1+1}"
echo "Bouwen van $VERSION_NAME ($VERSION_CODE)..."
./gradlew assembleStstableRelease -q

OUT=smarttubetv/build/outputs/apk/ststable/release
SITE=build/update-site/smarttube
rm -rf "$SITE"; mkdir -p "$SITE"
for abi in armeabi-v7a arm64-v8a x86 universal; do
    cp "$OUT/SmartTube_stable_${VERSION_NAME}_${abi}.apk" "$SITE/smarttube_${VERSION_CODE}_${abi}.apk"
done

python -I - "$SITE" "$BASE_URL" "$VERSION_NAME" "$VERSION_CODE" "$@" <<'EOF'
import json, sys
site, base, name, code = sys.argv[1], sys.argv[2], sys.argv[3], int(sys.argv[4])
changelog = sys.argv[5:]
def url(abi): return "%s/smarttube_%d_%s.apk" % (base, code, abi)
manifest = {
    "package": {
        "downloadUrl": url("universal"),
        "downloadUrlList": [url("universal")],
        "downloadUrlList_armeabi-v7a": [url("armeabi-v7a")],
        "downloadUrlList_arm64-v8a": [url("arm64-v8a")],
        "downloadUrlList_x86": [url("x86")],
    },
    name: {"versionCode": code, "changelog": changelog, "changelog_nl": changelog},
}
open("%s/stable.json" % site, "w", encoding="utf-8").write(json.dumps(manifest, indent=2, ensure_ascii=False))
print("stable.json: versie %s (%d), %d regel(s) changelog" % (name, code, len(changelog)))
EOF

SHARE="/c/Users/Gebruiker/Downloads/SmartTube_Inge"
if [ -d "$SHARE" ] && [ -z "$NO_SHARE" ]; then
    cp "$OUT/SmartTube_stable_${VERSION_NAME}_armeabi-v7a.apk" "$SHARE/SmartTube_Inge_Philips.apk"
    cp "$OUT/SmartTube_stable_${VERSION_NAME}_universal.apk" "$SHARE/SmartTube_Inge.apk"
    rm -f "$SHARE"/SmartTube_Inge_32.64*.apk
    echo "Deelmap bijgewerkt: $SHARE"
fi

if [ -z "$NO_UPLOAD" ]; then
    echo "Publiceren als GitHub Release ${TAG} in ${REPO} ..."
    NOTES=$(printf -- "- %s\n" "$@")
    gh release create "$TAG" "$SITE"/*.apk "$SITE/stable.json" \
        --repo "$REPO" --title "SmartTube Inge ${VERSION_NAME}" --notes "$NOTES" --latest
    echo "Gepubliceerd: $VERSION_NAME ($VERSION_CODE). Tv's krijgen de update bij de volgende start van de app."
else
    echo "Klaargezet in $SITE (niet gepubliceerd)."
fi
