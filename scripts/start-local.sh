#!/bin/bash
sudo service postgresql restart
sudo sysctl -w kernel.apparmor_restrict_unprivileged_userns=0
sudo service docker restart

export ADOPTU_ENV=dev

# Remove existing mailpit container if left over from a previous run
docker rm -f mailpit 2>/dev/null || true
docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit

# Start LocalStack community edition (S3 + SES, no auth token required)
docker run --rm -it \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e EXTRA_CORS_ALLOWED_ORIGINS="http://localhost:8080" \
  -p 4566:4566 -p 4510-4559:4510-4559 \
  localstack/localstack:4.5.0
