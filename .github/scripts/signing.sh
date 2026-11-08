#!/usr/bin/env bash
# Escribe keystore.properties en la carpeta del proyecto ($1) para firmar el APK de lanzamiento.
# Con la llave guardada en los secretos de GitHub se firma siempre con la misma (se puede actualizar
# encima de la versión anterior). Sin ella se usa una llave temporal: el APK se instala, pero la
# siguiente versión llevará otra firma y habrá que desinstalar la anterior.
set -euo pipefail
dir="$1"
jks="$RUNNER_TEMP/tube.jks"
if [ -n "${KEYSTORE_BASE64:-}" ]; then
  printf '%s' "$KEYSTORE_BASE64" | base64 --decode > "$jks"
  alias="$KEY_ALIAS"; storepass="$KEYSTORE_PASSWORD"; keypass="$KEY_PASSWORD"
else
  echo "::warning::No hay llave de firma en los secretos de GitHub; se firma con una llave temporal."
  alias="tube"; storepass="tubeottmty"; keypass="tubeottmty"
  keytool -genkeypair -keystore "$jks" -alias "$alias" -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$storepass" -keypass "$keypass" -dname "CN=Tube OTTMTY, O=OTTMTY" >/dev/null 2>&1
fi
{
  echo "storeFile=$jks"
  echo "storePassword=$storepass"
  echo "keyAlias=$alias"
  echo "keyPassword=$keypass"
} > "$dir/keystore.properties"
