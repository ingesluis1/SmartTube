# SmartTube (Inge's build)

Aangepaste versie van [SmartTube](https://github.com/yuliskov/SmartTube) door yuliskov, onder dezelfde MIT-licentie.
Deze build lijkt meer op de officiële YouTube-app voor tv:

- opgeruimde tegels zonder zoom bij focus, navigatiestrook en menu in YouTube-stijl
- eenvoudigere spelerbalk, kanaalpaneel in de speler, overzichtelijke reacties
- "Wie kijkt er?"-profielkiezer met achtergrondkleur uit de profielfoto
- zoekscherm met letterraster en zoekgeschiedenis zoals de YouTube-app
- snellere start (beveiligingstoken wordt bewaard en vooraf aangemaakt)
- updates via de [releases](https://github.com/ingesluis1/SmartTube/releases) van deze repository

Alle wijzigingen zijn in de code gemarkeerd met `MOD (Inge)`.

**Installeren:** download de nieuwste apk onder Releases (`armeabi-v7a` voor de meeste tv's) en installeer die met de app Downloader.

**Zelf bouwen:** JDK 17, dan `./gradlew assembleStstableDebug`. Voor een release-build heb je een eigen `keystore.properties` met ondertekeningssleutel nodig (staat niet in deze repository).

**Let op:** ik heb deze versie alleen getest op een Android TV-emulator en op een Philips 65OLED810 (Google TV). Op andere tv's of boxen kan het anders werken. Ik maak dit voor mezelf en geef geen support: kom je een bug tegen, fork de repository dan gerust en los het zelf op. Meld problemen met deze versie ook niet bij de oorspronkelijke maker van SmartTube.

Meer over de originele app, zoals alle functies en ondersteunde apparaten, staat bij [yuliskov/SmartTube](https://github.com/yuliskov/SmartTube).
