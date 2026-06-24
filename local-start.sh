#!/bin/bash
sudo service postgresql restart
sudo sysctl -w kernel.apparmor_restrict_unprivileged_userns=0
sudo service docker restart
docker -H unix:///run/docker.sock compose --profile dev up -d
