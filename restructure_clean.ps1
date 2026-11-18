*** Begin PowerShell Script ***
# Restructure the Reliable Job Queue repo and rewrite commit history with natural‑sentence messages.
# Run from the repository root (d:\reliable-job-queue).

# ------------------------------------------------------------
# 1️⃣ Safety backup
# ------------------------------------------------------------
git branch backup_before_cleanup

# ------------------------------------------------------------
# 2️⃣ Start a fresh orphan branch (clean history)
# ------------------------------------------------------------
git checkout --orphan main
git rm -rf --cached .
git reset --hard

# ------------------------------------------------------------
# 3️⃣ Helper to commit a set of files with a clear message
# ------------------------------------------------------------
function Commit-Set($Paths, $Message) {
    foreach ($p in $Paths) { git add $p }
    git commit -m $Message
}

# ------------------------------------------------------------
# 4️⃣ Move files into a more conventional layout
# ------------------------------------------------------------
# Create directories for scripts and docker assets
New-Item -ItemType Directory -Force -Path scripts\cli
New-Item -ItemType Directory -Force -Path docker

# Move CLI helper scripts
git mv cli.ps1 scripts\cli\cli.ps1
git mv cli.sh scripts\cli\cli.sh

# Move Dockerfile and compose file
git mv Dockerfile docker\Dockerfile
git mv docker-compose.yml docker\docker-compose.yml

# (Optional) Keep other config files where they are – they already follow Maven conventions.

# ------------------------------------------------------------
# 5️⃣ Commit the new project scaffold (Maven + Spring Boot)
# ------------------------------------------------------------
Commit-Set @(
    "pom.xml",
    "src/main/resources/application.yml"
) "Create a new Maven project and set up Spring Boot"

# ------------------------------------------------------------
# 6️⃣ Core domain model and API
# ------------------------------------------------------------
Commit-Set @(
    "src/main/java/com/example/job/Job.java",
    "src/main/java/com/example/job/JobRepository.java",
    "src/main/java/com/example/controller/JobController.java"
) "Add the job entity, repository and a simple API to enqueue jobs"

# ------------------------------------------------------------
# 7️⃣ Worker lease and atomic claim logic
# ------------------------------------------------------------
Commit-Set @(
    "src/main/java/com/example/worker/WorkerService.java",
    "src/main/java/com/example/worker/Lease.java"
) "Implement atomic job claiming with SKIP LOCKED and a worker‑lease system"

# ------------------------------------------------------------
# 8️⃣ Retry policy and dead‑letter queue
# ------------------------------------------------------------
Commit-Set @(
    "src/main/java/com/example/retry/RetryPolicy.java",
    "src/main/java/com/example/dlq/DLQService.java"
) "Add exponential back‑off retries and a dead‑letter queue"

# ------------------------------------------------------------
# 9️⃣ DLQ replay endpoint
# ------------------------------------------------------------
Commit-Set @(
    "src/main/java/com/example/controller/DLQController.java"
) "Expose an endpoint to replay jobs from the dead‑letter queue"

# ------------------------------------------------------------
# 🔟 Idempotency support
# ------------------------------------------------------------
Commit-Set @(
    "src/main/java/com/example/idempotency/IdempotencyService.java"
) "Introduce an idempotency ledger to avoid duplicate processing"

# ------------------------------------------------------------
# 1️⃣1️⃣ Job dependency handling
# ------------------------------------------------------------
Commit-Set @(
    "src/main/java/com/example/dependency/JobDependencyService.java"
) "Add support for job dependencies so a job can wait for another"

# ------------------------------------------------------------
# 1️⃣2️⃣ Docker assets
# ------------------------------------------------------------
Commit-Set @(
    "docker/Dockerfile",
    "docker/docker-compose.yml"
) "Add Dockerfile and a Compose file for easy local deployment"

# ------------------------------------------------------------
# 1️⃣3️⃣ Cloud infrastructure scripts
# ------------------------------------------------------------
Commit-Set @(
    "aws/cloudformation.yml",
    "terraform/main.tf"
) "Include CloudFormation and Terraform scripts for AWS deployment"

# ------------------------------------------------------------
# 1️⃣4️⃣ Web dashboard UI
# ------------------------------------------------------------
Commit-Set @(
    "src/main/resources/static/**",
    "src/main/resources/templates/**"
) "Create a simple web dashboard for monitoring the queue"

# ------------------------------------------------------------
# 1️⃣5️⃣ CLI helper scripts (now under scripts/cli)
# ------------------------------------------------------------
Commit-Set @(
    "scripts/cli/cli.ps1",
    "scripts/cli/cli.sh"
) "Provide PowerShell and Bash scripts to inspect the queue locally"

# ------------------------------------------------------------
# 1️⃣6️⃣ Documentation
# ------------------------------------------------------------
Commit-Set @(
    "README.md"
) "Write a clear README with overview, quick‑start and usage details"

# ------------------------------------------------------------
# 1️⃣7️⃣ CI workflow
# ------------------------------------------------------------
Commit-Set @(
    ".github/workflows/ci.yml"
) "Add a GitHub Actions workflow that builds and runs the test suite"

# ------------------------------------------------------------
# 6️⃣ Final reminder – push the new history
# ------------------------------------------------------------
Write-Host "History rewritten – run the following commands to push:" 
Write-Host "git remote add origin https://github.com/cipher-4/distributed-job-queue.git" 
Write-Host "git push -u origin main"
*** End PowerShell Script ***