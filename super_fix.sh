#!/bin/bash
echo "🚀 INICIANDO REPARACIÓN TOTAL DE EDUK..."

# 1. Definir rutas absolutas de Codespaces
export ANDROID_HOME=/home/codespace/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 2. Crear carpetas y descargar herramientas si no existen
mkdir -p $ANDROID_HOME/cmdline-tools
if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
    echo "📥 Descargando herramientas de línea de comandos de Android..."
    cd $ANDROID_HOME/cmdline-tools
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
    unzip -q commandlinetools-linux-11076708_latest.zip
    mv cmdline-tools latest
    rm commandlinetools-linux-11076708_latest.zip
    cd /workspaces/Eduk
fi

# 3. Instalar versiones específicas para evitar el error 25.0.2
echo "🛠 Instalando Build Tools 34.0.0 y Platform 34..."
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 4. Forzar local.properties con ruta absoluta
echo "sdk.dir=/home/codespace/android-sdk" > local.properties

# 5. Limpiar y construir con logs detallados
echo "🏗 Construyendo aplicación..."
chmod +x gradlew
./gradlew clean assembleDebug --info
