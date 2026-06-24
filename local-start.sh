#!/bin/bash
sudo service postgresql restart
sudo sysctl -w kernel.apparmor_restrict_unprivileged_userns=0
sudo service docker restart
docker -H unix:///run/docker.sock run -e EXTRA_CORS_ALLOWED_ORIGINS="http://localhost:8080" --rm -it -p 4566:4566 -p 4510-4559:4510-4559 localstack/localstack:4.0.0
