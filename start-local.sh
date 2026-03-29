#!/bin/bash
sudo service postgresql restart
sudo sysctl -w kernel.apparmor_restrict_unprivileged_userns=0
sudo service docker restart
docker run --rm -it -p 4566:4566 -p 4510-4559:4510-4559 localstack/localstack
