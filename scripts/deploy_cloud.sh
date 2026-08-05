#!/usr/bin/env bash
# Despliegue del backend Cloud de Alexandria (Functions + Firestore + App Check).
# Proyecto Firebase: alexandria-d3397
#
# USO:
#   1) bash scripts/deploy_cloud.sh setup       # login + use --add + npm install + secrets
#   2) bash scripts/deploy_cloud.sh deploy      # firebase deploy --only functions
#   3) bash scripts/deploy_cloud.sh appcheck    # imprime los SHA-1 para App Check (paso manual en consola)
#
# NOTA: el paso "Activar App Check (Play Integrity)" es MANUAL en la Firebase Console
# (App Check → Play Integrity → añadir SHA-1 debug y release). Imprímelos con:
#   bash scripts/deploy_cloud.sh appcheck

set -euo pipefail

PROJECT="alexandria-d3397"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FUNCTIONS_DIR="$DIR/functions"

DEBUG_KEYSTORE="$HOME/.android/debug.keystore"
RELEASE_KEYSTORE="$DIR/app/keystore/alexandria-release.jks"

usage() { grep '^#' "$0" | head -20 | sed 's/^# //'; }

cmd_appcheck() {
  echo "=== SHA-1 para App Check (Play Integrity) ==="
  echo "DEBUG  ($DEBUG_KEYSTORE):"
  keytool -list -v -keystore "$DEBUG_KEYSTORE" -alias androiddebugkey -storepass android 2>/dev/null \
    | grep -i "SHA1:" | head -1 || echo "  (no encontrado — instala Android Studio)"
  echo ""
  echo "RELEASE ($RELEASE_KEYSTORE):"
  if [ -f "$RELEASE_KEYSTORE" ] && [ -f "$DIR/keystore.properties" ]; then
    SP="$(grep '^storePassword=' "$DIR/keystore.properties" | cut -d= -f2)"
    ALIAS="$(grep '^keyAlias=' "$DIR/keystore.properties" | cut -d= -f2)"
    keytool -list -v -keystore "$RELEASE_KEYSTORE" -alias "$ALIAS" -storepass "$SP" 2>/dev/null \
      | grep -i "SHA1:" | head -1 || echo "  (no válido — ver keystore.properties)"
  else
    echo "  (falta keystore.properties o release keystore)"
  fi
  echo ""
  echo "PASO MANUAL: Firebase Console → App Check → Play Integrity → Registra la app"
  echo "  app: com.alexandria.app  |  project: $PROJECT"
  echo "  Añade AMBOS SHA-1 (debug y release). En enforcement, empieza con 'Monitorear'."
}

cmd_setup() {
  if ! command -v firebase >/dev/null 2>&1; then
    echo "Instalando firebase-tools..."; npm install -g firebase-tools
  fi
  cd "$FUNCTIONS_DIR"
  echo "=== firebase login (abre navegador; si no hay navegador usa --no-localhost) ==="
  firebase login --no-localhost
  echo "=== firebase use --add ==="
  firebase use --add
  echo "=== npm install ==="
  npm install
  echo "=== Secrets (se te pedirán OPENROUTER_API_KEY y GOOGLE_BOOKS_API_KEY) ==="
  firebase functions:secrets:set OPENROUTER_API_KEY --project "$PROJECT" --force
  firebase functions:secrets:set GOOGLE_BOOKS_API_KEY --project "$PROJECT" --force
  echo "Setup completo. Siguiente: Activar App Check (bash scripts/deploy_cloud.sh appcheck)"
}

cmd_deploy() {
  cd "$FUNCTIONS_DIR"
  npm run build
  firebase deploy --only functions --project "$PROJECT" --force
  echo "=== Deploy OK. Recuerda activar App Check (Play Integrity). ==="
}

case "${1:-}" in
  setup)    cmd_setup ;;
  deploy)   cmd_deploy ;;
  appcheck) cmd_appcheck ;;
  *)        usage; exit 1 ;;
esac
