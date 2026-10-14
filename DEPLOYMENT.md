# 🚀 CI/CD & Deployment Guide for CinetPay

This guide explains how to set up continuous integration, containerization, and automated deployments for this Spring Boot CinetPay application using **GitHub Actions**, **Docker**, and **Docker Compose** on a Linux VPS / Cloud VM.

---

## 🏗️ Architecture Overview

```
                                 +-----------------------+
                                 |  Developer Git Push   |
                                 +-----------+-----------+
                                             |
                                             v
                           +-----------------------------------+
                           | GitHub Actions CI Pipeline        |
                           |  - JDK 17 Setup & Maven Cache     |
                           |  - Automated Unit & Smoke Tests   |
                           |  - Packaging & Docker Validation  |
                           +-----------------+-----------------+
                                             | (on main/master push)
                                             v
                           +-----------------------------------+
                           | GitHub Actions CD Pipeline        |
                           |  - Build Multi-Stage Docker Image |
                           |  - Push to GitHub Container Reg.  |
                           |    (ghcr.io/ritish-dollop/cinet-pay)
                           +-----------------+-----------------+
                                             | SSH Trigger
                                             v
                           +-----------------------------------+
                           | Remote Linux VPS / Cloud VM       |
                           |  - Sync compose & deploy.sh       |
                           |  - Pull latest Docker image       |
                           |  - Zero-downtime container update |
                           |  - Health check (/api/health)     |
                           |  - Automatic cleanup & rollback   |
                           +-----------------------------------+
```

---

## 📁 Pipeline Components

| File | Purpose |
| :--- | :--- |
| [`.github/workflows/ci.yml`](.github/workflows/ci.yml) | Continuous Integration: runs automated tests on PRs and pushes. |
| [`.github/workflows/deploy.yml`](.github/workflows/deploy.yml) | Continuous Deployment: builds image, pushes to GHCR, and deploys to VPS via SSH. |
| [`Dockerfile`](Dockerfile) | Multi-stage build with JDK 17, non-root user (`appuser`), and container JVM optimizations. |
| [`docker-compose.yml`](docker-compose.yml) | Orchestrates `cinetpay-app` and `mysql:8.0` with health dependencies and volumes. |
| [`deploy.sh`](deploy.sh) | Server-side deployment runner with container restart, health polling, and diagnostics. |
| [`.env.example`](.env.example) | Template with required and optional environment variables. |
| [`HealthController.java`](src/main/java/com/cinetpay/controller/HealthController.java) | Health check endpoint (`/api/health`) for container and CD probes. |

---

## ⚙️ Step 1: Prepare Your Linux Server

Run these commands once on your Linux VPS (Ubuntu/Debian) to set up Docker and Docker Compose:

```bash
# 1. Update system packages
sudo apt update && sudo apt upgrade -y

# 2. Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 3. Add current user to docker group (run without sudo)
sudo usermod -aG docker $USER
newgrp docker

# 4. Verify Docker and Docker Compose
docker --version
docker compose version
```

---

## 🔑 Step 2: Configure SSH Access for GitHub Actions

### 1. Generate a dedicated deployment SSH key pair (on your local machine or server):
```bash
ssh-keygen -t ed25519 -C "github-actions-cinetpay" -f cinetpay_deploy_key -N ""
```

### 2. Add the public key to your server:
Copy the contents of `cinetpay_deploy_key.pub` into `~/.ssh/authorized_keys` on your server:
```bash
cat cinetpay_deploy_key.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

### 3. Add Secrets in GitHub:
Go to your GitHub repository -> **Settings** -> **Secrets and variables** -> **Actions** -> **New repository secret**:

| Secret Name | Value | Required |
| :--- | :--- | :---: |
| `SERVER_HOST` | IP address or domain of your VPS (e.g. `203.0.113.10`) | **Yes** |
| `SERVER_USER` | SSH username (e.g. `ubuntu` or `root`) | **Yes** |
| `SERVER_SSH_KEY` | Private key contents (`cinetpay_deploy_key`) | **Yes** |
| `SERVER_PORT` | SSH port (defaults to `22` if not provided) | Optional |

---

## 🔐 Step 3: Configure Application Environment on Server

Create your production environment file on the server:

```bash
mkdir -p ~/cinetpay-app
cd ~/cinetpay-app
nano .env
```

Paste and fill in your real credentials (refer to [`.env.example`](.env.example)):

```bash
APP_IMAGE=ghcr.io/ritish-dollop/cinet-pay:latest
APP_PORT=8080

# MySQL Credentials
DB_NAME=cinetpay_db
DB_USERNAME=cinetuser
DB_PASSWORD=StrongDatabasePassword123!
DB_ROOT_PASSWORD=StrongRootPassword123!

# CinetPay Credentials
CINETPAY_API_KEY=your_cinetpay_api_key
CINETPAY_API_PASSWORD=your_cinetpay_api_password
CINETPAY_BASE_URL=https://api.cinetpay.net
CINETPAY_SUCCESS_URL=https://yourdomain.com/api/payment/success
CINETPAY_FAILED_URL=https://yourdomain.com/api/payment/failed
CINETPAY_NOTIFY_URL=https://yourdomain.com/api/payment/notify

# Webhook Secret (HMAC SHA256)
WEBHOOK_SECRET=your_secret_webhook_key
```

Save with `Ctrl + O` and exit with `Ctrl + X`.

---

## 🚀 Step 4: Triggering Deployments

### Automated Deployment
Pushing commits to the `master` branch will automatically:
1. Run Maven compile and tests.
2. Build and publish the Docker container to `ghcr.io/ritish-dollop/cinet-pay:latest`.
3. Connect via SSH to your VPS and invoke `./deploy.sh`.
4. Verify HTTP 200 response on `/api/health`.

### Manual Deployment
Go to **Actions** in GitHub -> Select **CD - Build, Containerize & Deploy** -> Click **Run workflow**.

---

## 💻 Local Testing with Docker Compose

If you have Docker installed locally, you can spin up the full stack (App + MySQL) with a single command:

```bash
# 1. Copy sample environment file
cp .env.example .env

# 2. Build and start containers
docker compose up --build -d

# 3. Check logs
docker compose logs -f app

# 4. Check health endpoint
curl http://localhost:8080/api/health

# 5. Stop containers
docker compose down
```
