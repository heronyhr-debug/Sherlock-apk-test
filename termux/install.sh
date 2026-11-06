#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
pkg update
pkg install -y python
python -m pip install sherlock-project
termux-setup-storage
mkdir -p "$HOME/sherlock-android" "$HOME/storage/downloads/SherlockResults" "$HOME/.termux"
cp "$(dirname "$0")/runner.py" "$HOME/sherlock-android/runner.py"
chmod 700 "$HOME/sherlock-android/runner.py"
if grep -q '^allow-external-apps=' "$HOME/.termux/termux.properties" 2>/dev/null; then
 sed -i 's/^allow-external-apps=.*/allow-external-apps=true/' "$HOME/.termux/termux.properties"
else
 echo 'allow-external-apps=true' >> "$HOME/.termux/termux.properties"
fi
termux-reload-settings
python -m sherlock_project --help >/dev/null
echo "完成。请手动授予 APK 在 Termux 运行命令的额外权限。"
