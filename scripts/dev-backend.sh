#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/dev-common.sh"
load_dev_environment
unset SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD
unset SPRING_DATA_REDIS_HOST SPRING_DATA_REDIS_PORT SPRING_DATA_REDIS_USERNAME SPRING_DATA_REDIS_PASSWORD
cd "$DEV_ROOT/backend"
exec ./mvnw -B -Dmaven.test.skip=true -Dspring-boot.run.profiles=dev spring-boot:run
