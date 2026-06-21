#!/bin/bash
sudo service postgresql restart
sudo sysctl -w kernel.apparmor_restrict_unprivileged_userns=0
sudo service docker restart
docker -H unix:///run/docker.sock run -e LOCALSTACK_AUTH_TOKEN="ls-xEbE8734-fUqu-kOme-zOpA-1482XITi3c2e" -e EXTRA_CORS_ALLOWED_ORIGINS="http://localhost:8080" --rm -it -p 4566:4566 -p 4510-4559:4510-4559 localstack/localstack
