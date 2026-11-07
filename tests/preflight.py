from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
java=root/'app/src/main/java/com/johna/assistant'
for n in ['MainActivity.kt','GlassDashboard.kt','HolographicDashboard.kt','JohnaSphere.kt','WakePhraseParser.kt','LocalVolumeParser.kt','WeatherForecast.kt','WeatherDetailPanel.kt']:
 assert (java/n).exists(),n
ET.parse(root/'app/src/main/AndroidManifest.xml')
gradle=(root/'app/build.gradle.kts').read_text()
assert 'versionCode = 117' in gradle and 'versionName = "1.0.16"' in gradle
assert 'material-icons-extended' in gradle
activity=(java/'MainActivity.kt').read_text()
glass=(java/'GlassDashboard.kt').read_text()
holographic=(java/'HolographicDashboard.kt').read_text()
orb=(java/'JohnaSphere.kt').read_text()
assert 'onVoiceShortcut = { command' in activity and 'onVoiceShortcut: (String) -> Unit' in glass
assert 'HolographicHome(' in glass and 'fun HolographicHome(' in holographic
assert 'BackHandler(' in glass and 'AndroidView(' in glass
assert 'createOnDeviceSpeechRecognizer' in activity and 'disconnectCount >= 3' in activity
assert 'wakeEnabled=false' in activity
assert 'onQuickCommand("volume down 2")' in holographic
assert 'JohnaSphere(speaking,listening' in holographic
assert 'drawOval' in orb and 'rotate(-24f' in orb
assert 'onVoiceShortcut' in activity and 'onQuickCommand' in holographic
assert 'preview/index.html' in {p.relative_to(root).as_posix() for p in root.rglob('*.html')}
print('PASS: 17 structural, code wiring, Kotlin source, Android manifest, and UI assertions')
