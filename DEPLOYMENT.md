# 🚀 Direct JAR CI/CD & Deployment Guide

This pipeline builds and deploys your Spring Boot CinetPay application directly to a Linux server as a JAR service (without Docker).

---

## 🏗️ Workflow Overview

Every commit pushed to `master` triggers the `deploy` pipeline in GitHub Actions:

```
[Checkout Repository]
        ↓
[Setup Java 17]
        ↓
[Build Application (mvnw package)]
        ↓
[Prepare Deployment (target/cinet-pay.jar)]
        ↓
[Setup SSH]
        ↓
[Add Server to Known Hosts]
        ↓
[Ensure Remote Directory Exists (~/cinetpay-app)]
        ↓
[Upload JAR (via SCP)]
        ↓
[Restart Backend Service (systemctl / java -jar)]
        ↓
[Verify Health (/api/health)]
        ↓
[Deployment Successful]
```

---

## 📁 Pipeline Files

| File | Description |
| :--- | :--- |
| [`.github/workflows/deploy.yml`](.github/workflows/deploy.yml) | Direct JAR build, test, upload, and restart pipeline. |
| [`cinetpay.service`](cinetpay.service) | Linux systemd service unit file for auto-start. |
| [`HealthController.java`](src/main/java/com/cinetpay/controller/HealthController.java) | Health check endpoint (`/api/health`) verified during deployment. |

---

## 🔑 GitHub Secrets Configuration

Under **Settings** ➔ **Secrets and variables** ➔ **Actions**:

- `SERVER_HOST`: Public IP of your Linux server.
- `SERVER_USER`: SSH username (`ubuntu` or `root`).
- `SERVER_SSH_KEY`: Private SSH Key.
- `SERVER_PORT`: `22` *(optional)*.

*(If `SERVER_HOST` is not set yet, the workflow will build and verify the application without attempting the remote SSH upload).*

---

## 🖥️ Server Setup (Ubuntu / Debian)

On your Linux server, run once:

```bash
# 1. Install Java 17 and MySQL
sudo apt update
sudo apt install -y openjdk-17-jre mysql-server

# 2. Create app directory
mkdir -p ~/cinetpay-app

# 3. Setup systemd service (optional, recommended)
sudo cp cinetpay.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable cinetpay
```
