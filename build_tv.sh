#!/bin/bash
# Bouwt SmartTube (stabiele debug-variant) en zet de universele APK klaar voor de tv.
# Gebruik: ./build_tv.sh   daarna op de tv in Downloader: http://192.168.1.159:8000/smarttube.apk
set -e
cd "$(dirname "$0")"
export JAVA_HOME="C:/Users/Gebruiker/AndroidStudioProjects/tools/jdk-17.0.20.1+1"
./gradlew assembleStstableDebug -q
APK=$(ls smarttubetv/build/outputs/apk/ststable/debug/*_universal.apk | head -1)
DIST="C:/Users/Gebruiker/AndroidStudioProjects/TvKanalen/dist"
cp "$APK" "$DIST/smarttube.apk.tmp" && mv "$DIST/smarttube.apk.tmp" "$DIST/smarttube.apk"
ls -l "$DIST/smarttube.apk"
echo "Klaar. Webserver (indien nog niet actief): cd \"$DIST\" && python -m http.server 8000 --bind 0.0.0.0"
