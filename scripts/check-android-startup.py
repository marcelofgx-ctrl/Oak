"""Regression check on a connected disposable Android emulator; never use a user's phone."""
import os
import pathlib
import subprocess
import sys
import time

apk = pathlib.Path(sys.argv[1]).resolve()
sdk = pathlib.Path(os.environ['ANDROID_HOME'])
adb = str(sdk / 'platform-tools/adb')
serial = subprocess.check_output([adb, 'get-serialno'], text=True).strip()
if not serial.startswith('emulator-'):
    raise SystemExit('Use a disposable emulator, not a physical device.')

def run(*args):
    return subprocess.check_output([adb, '-s', serial, *args], text=True, stderr=subprocess.STDOUT)

manifest = subprocess.check_output([str(sdk/'build-tools/36.0.0/aapt'), 'dump', 'xmltree', str(apk), 'AndroidManifest.xml'], text=True)
activity_names = []
in_activity = False
for line in manifest.splitlines():
    if line.startswith('      E:'):
        in_activity = line.strip().startswith('E: activity ')
    elif in_activity and line.startswith('        A: android:name'):
        activity_names.append(line.split('="', 1)[1].split('"', 1)[0])
if 'com.google.androidbrowserhelper.trusted.ManageDataLauncherActivity' not in activity_names:
    raise SystemExit('Required ManageDataLauncherActivity missing from packaged manifest.')
print(run('install', '-r', str(apk)).strip())
for attempt in range(2):
    run('shell', 'am', 'force-stop', 'com.oakandember.operator')
    run('logcat', '-c', '-b', 'crash')
    result = run('shell', 'am', 'start', '-W', '-n', 'com.oakandember.operator/.MainActivity')
    if 'Status: ok' not in result:
        raise SystemExit('Activity did not start: ' + result)
    time.sleep(5)
    crash = run('logcat', '-d', '-b', 'crash')
    if 'Process: com.oakandember.operator' in crash:
        raise SystemExit(crash)
    print(f'PASS: cold launch {attempt+1}, no Oak startup crash.')
print('PASS: same-package APK update and startup regression. Browser/push delivery require a compatible Chrome.')
