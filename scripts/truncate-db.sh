#!/usr/bin/env bash
set -euo pipefail

# Database connection defaults (matches application.conf / docker-compose.yml)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-adoptu}"
DB_USER="${DB_USER:-adoptu}"
DB_PASSWORD="${DB_PASSWORD:-Ad0ptU}"
export PGPASSWORD="$DB_PASSWORD"

usage() {
  echo "Usage: $0 [--yes]"
  echo ""
  echo "Truncates all application tables in the $DB_NAME database."
  echo "Override connection via env vars: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD"
  echo ""
  echo "  --yes   Skip confirmation prompt"
  exit 1
}

SKIP_CONFIRM=false
for arg in "$@"; do
  case "$arg" in
    --yes) SKIP_CONFIRM=true ;;
    --help|-h) usage ;;
    *) echo "Unknown argument: $arg"; usage ;;
  esac
done

echo "Target: ${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME}"

if [[ "$SKIP_CONFIRM" == false ]]; then
  read -rp "This will DELETE ALL DATA from all tables. Continue? [y/N] " confirm
  [[ "$confirm" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 0; }
fi

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" <<'SQL'
TRUNCATE TABLE
  sterilization_locations,
  animal_shelters,
  temporal_home_requests,
  blocked_rescuers,
  temporal_homes,
  photography_requests,
  adoption_requests,
  pet_images,
  pets,
  webauthn_credentials,
  user_active_roles,
  photographers,
  email_verification_attempts,
  email_change_tokens,
  password_reset_tokens,
  magic_link_tokens,
  user_passwords,
  email_verification_tokens,
  users
CASCADE;
SQL

echo "Done. All tables truncated."
