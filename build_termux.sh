#!/bin/bash
# ═══════════════════════════════════════════════════════════════
#  FANTER Keyboard — اسکریپت ساخت ترموکس
#  فقط یک‌بار اجرا کن — همه چیز خودکار نصب میشه
# ═══════════════════════════════════════════════════════════════
set -e

ANDROID_HOME="$HOME/android-sdk"
BUILD_TOOLS="$ANDROID_HOME/build-tools/34.0.0"
PLATFORM="$ANDROID_HOME/platforms/android-34"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "══════════════════════════════════"
echo "  FANTER Keyboard Builder"
echo "══════════════════════════════════"

# ── نصب Java ────────────────────────────────────────────────
if ! command -v java &>/dev/null; then
    echo "📦 نصب Java..."
    pkg install -y openjdk-17
fi
echo "✓ Java: $(java -version 2>&1 | head -1)"

# ── نصب wget/unzip ──────────────────────────────────────────
pkg install -y wget unzip 2>/dev/null || true

# ── دانلود Android SDK ──────────────────────────────────────
if [ ! -f "$BUILD_TOOLS/aapt" ] && [ ! -f "$BUILD_TOOLS/aapt.exe" ]; then
    echo "📥 دانلود Android Command-line Tools..."
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    cd "$ANDROID_HOME/cmdline-tools"
    
    # نسخه جدیدتر برای لینوکس
    TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    wget -q --show-progress "$TOOLS_URL" -O tools.zip
    unzip -q tools.zip
    mv cmdline-tools latest 2>/dev/null || true
    rm -f tools.zip
    
    export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin"
    
    echo "📱 نصب Android SDK (پلتفرم و بیلد-تولز)..."
    yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses > /dev/null 2>&1 || true
    sdkmanager --sdk_root="$ANDROID_HOME" \
        "platforms;android-34" \
        "build-tools;34.0.0"
fi

export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$BUILD_TOOLS:$ANDROID_HOME/platform-tools"

echo "✓ Build tools آماده"

# ── Gradle ──────────────────────────────────────────────────
cd "$PROJECT_DIR"

if [ ! -f "gradlew" ]; then
    echo "📥 دانلود Gradle Wrapper..."
    GRADLE_VER="8.4"
    mkdir -p gradle/wrapper
    wget -q "https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VER}.0/gradle/wrapper/gradle-wrapper.jar" \
         -O gradle/wrapper/gradle-wrapper.jar 2>/dev/null || \
    wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VER}-bin.zip" \
         -O /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip "gradle-${GRADLE_VER}/bin/gradle" -d /tmp/ && \
    cp "/tmp/gradle-${GRADLE_VER}/bin/gradle" . || true
    
    # gradlew دستی
    cat > gradlew << 'GWEOF'
#!/bin/bash
exec java -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
GWEOF
fi
chmod +x gradlew

echo "🏗 در حال build..."
ANDROID_SDK_ROOT="$ANDROID_HOME" \
ANDROID_HOME="$ANDROID_HOME" \
./gradlew assembleDebug --no-daemon -q

# ── خروجی ───────────────────────────────────────────────────
APK="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK" ]; then
    cp "$APK" "$HOME/FanterKeyboard.apk"
    echo ""
    echo "══════════════════════════════════"
    echo "✅ ساخت موفق!"
    echo "📍 فایل: ~/FanterKeyboard.apk"
    echo "══════════════════════════════════"
    echo ""
    echo "نصب:"
    echo "  termux-open ~/FanterKeyboard.apk"
else
    echo "❌ خطا در ساخت — لاگ بالا را بررسی کن"
    exit 1
fi
