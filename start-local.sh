#!/bin/bash
sudo service postgresql restart
sudo sysctl -w kernel.apparmor_restrict_unprivileged_userns=0
sudo service docker restart

export ADOPTU_ENV=dev

if [ -z "${LOCALSTACK_AUTH_TOKEN}" ]; then
  echo "WARNING: LOCALSTACK_AUTH_TOKEN is not set. LocalStack will fail to start."
  echo "Get a token at https://app.localstack.cloud and run:"
  echo "  export LOCALSTACK_AUTH_TOKEN=<your-token>"
  exit 1
fi

# Remove existing mailpit container if left over from a previous run
docker rm -f mailpit 2>/dev/null || true
docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit

# Start LocalStack (mount Docker socket so it can manage its own containers)
docker run --rm -it \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e LOCALSTACK_AUTH_TOKEN="${LOCALSTACK_AUTH_TOKEN}" \
  -e EXTRA_CORS_ALLOWED_ORIGINS="http://localhost:8080" \
  -p 4566:4566 -p 4510-4559:4510-4559 \
  localstack/localstack
