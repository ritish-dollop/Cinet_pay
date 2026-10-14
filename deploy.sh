#!/usr/bin/env bash
# ==============================================================================
# Automated Deployment Script for CinetPay Application
# ==============================================================================
set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DEPLOY_DIR"

echo "=================================================="
echo "🚀 Starting CinetPay Deployment: $(date)"
echo "📍 Directory: $DEPLOY_DIR"
echo "=================================================="

# Determine Docker Compose Command (supports docker compose v2 and legacy docker-compose)
if docker compose version >/dev/null 2>&1; then
    DOCKER_COMPOSE="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
    DOCKER_COMPOSE="docker-compose"
else
    echo "❌ Error: Neither 'docker compose' nor 'docker-compose' was found on this system." >&2
    exit 1
fi

# Ensure .env exists
if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        echo "⚠️ Warning: .env not found. Creating from .env.example..."
        cp .env.example .env
        echo "⚠️ Please update .env with real production credentials."
    else
        echo "⚠️ Warning: No .env or .env.example found. Proceeding with system environment variables."
        touch .env
    fi
fi

# Verify Docker daemon is running
if ! docker info >/dev/null 2>&1; then
    echo "❌ Error: Docker daemon is not running." >&2
    exit 1
fi

# Export APP_IMAGE default if not set
export APP_IMAGE="${APP_IMAGE:-ghcr.io/ritish-dollop/cinet-pay:latest}"
echo "📦 Target Image: $APP_IMAGE"

echo "⬇️ Pulling latest container images..."
$DOCKER_COMPOSE pull app || echo "ℹ️ Pull skipped or local image will be used."

echo "🔄 Restarting containers..."
$DOCKER_COMPOSE up -d --remove-orphans

echo "⏳ Waiting for application health check at http://127.0.0.1:8080/api/health..."
HEALTH_URL="http://127.0.0.1:8080/api/health"
MAX_ATTEMPTS=24
ATTEMPT=1
IS_HEALTHY=0

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" -eq 200 ]; then
        echo "✅ Application responded HTTP 200 OK! Health check passed."
        IS_HEALTHY=1
        break
    else
        echo "[$ATTEMPT/$MAX_ATTEMPTS] Service not ready yet (HTTP $HTTP_CODE). Retrying in 5s..."
        sleep 5
        ATTEMPT=$((ATTEMPT + 1))
    fi
done

if [ $IS_HEALTHY -ne 1 ]; then
    echo "❌ Health check failed after $MAX_ATTEMPTS attempts." >&2
    echo "📋 Dumping recent container logs for debugging:"
    $DOCKER_COMPOSE logs --tail=100 app
    exit 1
fi

echo "🧹 Cleaning up dangling/unused images..."
docker image prune -f || true

echo "=================================================="
echo "🎉 Deployment completed successfully at $(date)!"
echo "=================================================="
