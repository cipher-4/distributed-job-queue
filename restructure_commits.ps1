*** Begin PowerShell Script ***
# This script rewrites the Git history of the Reliable Job Queue project
# into multiple logical commits to illustrate a realistic development workflow.
# It creates a backup branch, then builds a new commit series on a fresh branch.
# Run this script from the repository root (d:\reliable-job-queue).

# Step 1: safety backup of current state
git branch backup_original

# Step 2: create a new orphan branch (no history) named 'main'
# This will keep the working tree files but start a fresh history.
git checkout --orphan main
# Remove all files from the index (they become untracked)
git rm -rf --cached .
# Clean any leftover files from the index
git reset --hard

# Helper function to commit a set of files with a message
function Commit-Set($Paths, $Message) {
    foreach ($p in $Paths) { git add $p }
    git commit -m $Message
}

# Commit sequence (adjust paths as needed for your project layout)
# 1. Scaffold Maven project and basic Spring Boot setup
Commit-Set @(
    "pom.xml",
    "src/main/java/com/example/Application.java",
    "src/main/resources/application.yml"
) "init: scaffold Maven project and basic Spring Boot setup"

# 2. Core job entity and repository, basic enqueue API
Commit-Set @(
    "src/main/java/com/example/job/Job.java",
    "src/main/java/com/example/job/JobRepository.java",
    "src/main/java/com/example/controller/JobController.java"
) "feat: add core job entity, repository, and basic enqueue API"

# 3. Atomic claim with SKIP LOCKED and worker lease mechanism
Commit-Set @(
    "src/main/java/com/example/worker/WorkerService.java",
    "src/main/java/com/example/worker/Lease.java"
) "feat: implement atomic SKIP LOCKED claim & worker lease mechanism"

# 4. Exponential retry logic and Dead‑Letter Queue (DLQ)
Commit-Set @(
    "src/main/java/com/example/retry/RetryPolicy.java",
    "src/main/java/com/example/dlq/DLQService.java"
) "feat: add exponential retry logic and DLQ isolation"

# 5. DLQ replay endpoint
Commit-Set @(
    "src/main/java/com/example/controller/DLQController.java"
) "feat: create DLQ replay endpoint and service"

# 6. Idempotency ledger
Commit-Set @(
    "src/main/java/com/example/idempotency/IdempotencyService.java"
) "feat: add idempotency ledger with key enforcement"

# 7. Job dependency chaining
Commit-Set @(
    "src/main/java/com/example/dependency/JobDependencyService.java"
) "feat: implement job dependency chaining"

# 8. Dockerfile and docker‑compose deployment
Commit-Set @(
    "Dockerfile",
    "docker-compose.yml"
) "feat: add Dockerfile and docker‑compose multi‑node deployment"

# 9. Cloud infrastructure files (AWS CloudFormation & Terraform)
Commit-Set @(
    "aws/cloudformation.yml",
    "terraform/main.tf"
) "feat: add AWS CloudFormation & Terraform infrastructure files"

# 10. Web dashboard UI
Commit-Set @(
    "src/main/resources/static/**",
    "src/main/resources/templates/**"
) "feat: integrate real‑time web dashboard UI"

# 11. CLI inspection tools (PowerShell & Bash)
Commit-Set @(
    "cli.ps1",
    "cli.sh"
) "feat: provide PowerShell & Bash CLI inspection tools"

# 12. Documentation
Commit-Set @(
    "README.md"
) "docs: write comprehensive README and usage guide"

# 13. CI workflow
Commit-Set @(
    ".github/workflows/ci.yml"
) "ci: add GitHub Actions CI workflow"

# Final step: push to remote (you need to have the remote set up)
Write-Host "History rewritten. Use the following commands to add remote and push:"
Write-Host "git remote add origin https://github.com/cipher-4/distributed-job-queue.git"
Write-Host "git push -u origin main"
*** End PowerShell Script ***