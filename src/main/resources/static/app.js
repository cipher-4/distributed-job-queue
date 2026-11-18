// Reliable Job Queue Frontend App
let currentStatusFilter = "";

document.addEventListener("DOMContentLoaded", () => {
    fetchStats();
    fetchJobs();
    fetchDlq();

    // Polling intervals
    setInterval(fetchStats, 2000);
    setInterval(fetchJobs, 2500);
    setInterval(fetchDlq, 3500);

    // Event Listeners
    document.getElementById("refresh-btn").addEventListener("click", () => {
        fetchStats();
        fetchJobs();
        fetchDlq();
    });

    document.getElementById("trigger-claim-btn").addEventListener("click", triggerClaim);
    document.getElementById("enqueue-form").addEventListener("submit", handleEnqueue);
    document.getElementById("quick-demo-btn").addEventListener("click", runQuickFailureDemo);
    document.getElementById("replay-all-btn").addEventListener("click", replayAllDlq);

    // Filters
    document.querySelectorAll(".filter-btn").forEach(btn => {
        btn.addEventListener("click", (e) => {
            document.querySelectorAll(".filter-btn").forEach(b => b.classList.remove("active"));
            e.target.classList.add("active");
            currentStatusFilter = e.target.getAttribute("data-status");
            fetchJobs();
        });
    });
});

async function fetchStats() {
    try {
        const res = await fetch("/api/queue/stats");
        if (!res.ok) return;
        const data = await res.json();

        document.getElementById("stat-pending").textContent = data.pendingJobs;
        document.getElementById("stat-delayed").textContent = data.delayedScheduledJobs;
        document.getElementById("stat-running").textContent = data.runningJobs;
        document.getElementById("stat-completed").textContent = data.completedJobs;
        document.getElementById("stat-dlq").textContent = data.deadLetterJobs;
        document.getElementById("stat-unreplayed-dlq").textContent = `${data.unreplayedDlqJobs} unreplayed`;

        const badge = document.getElementById("worker-status-badge");
        if (data.workerActive) {
            badge.className = "badge badge-active";
            badge.textContent = `Worker: Active (${data.workerConcurrency} threads)`;
        } else {
            badge.className = "badge badge-danger";
            badge.textContent = `Worker: Inactive (${data.nodeMode})`;
        }
    } catch (err) {
        console.error("Failed to fetch stats", err);
    }
}

async function fetchJobs() {
    try {
        let url = "/api/jobs?size=15";
        if (currentStatusFilter) {
            url += `&status=${currentStatusFilter}`;
        }
        const res = await fetch(url);
        if (!res.ok) return;
        const page = await res.json();
        const tbody = document.getElementById("jobs-tbody");

        if (!page.content || page.content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center">No jobs found in queue.</td></tr>';
            return;
        }

        tbody.innerHTML = page.content.map(job => {
            const shortId = job.id.substring(0, 8);
            const statusClass = `status-${job.status}`;
            const retries = `${job.retryCount} / ${job.maxRetries}`;
            const worker = job.claimedBy ? `<span class="mono">${job.claimedBy}</span>` : "-";
            const dateStr = new Date(job.createdAt).toLocaleTimeString();
            let schedOrLease = "-";
            if (job.status === "RUNNING" && job.leaseExpiresAt) {
                const secLeft = Math.max(0, Math.round((new Date(job.leaseExpiresAt) - new Date()) / 1000));
                schedOrLease = `Lease: ${secLeft}s left`;
            } else if (job.scheduledAt) {
                const diff = Math.round((new Date(job.scheduledAt) - new Date()) / 1000);
                schedOrLease = diff > 0 ? `In ${diff}s` : "Immediate";
            }

            return `
                <tr>
                    <td class="mono" title="${job.id}">${shortId}...</td>
                    <td><strong>${job.jobType}</strong></td>
                    <td>${job.priority}</td>
                    <td><span class="badge ${statusClass}">${job.status}</span></td>
                    <td>${retries}</td>
                    <td>${schedOrLease}</td>
                    <td>${worker}</td>
                    <td class="mono" title="${escapeHtml(job.payload)}">${truncate(job.payload, 35)}</td>
                    <td>${dateStr}</td>
                </tr>
            `;
        }).join("");
    } catch (err) {
        console.error("Failed to fetch jobs", err);
    }
}

async function fetchDlq() {
    try {
        const res = await fetch("/api/dlq?size=10");
        if (!res.ok) return;
        const page = await res.json();
        const tbody = document.getElementById("dlq-tbody");

        if (!page.content || page.content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center">No dead letter jobs present.</td></tr>';
            return;
        }

        tbody.innerHTML = page.content.map(dlq => {
            const shortId = dlq.id.substring(0, 8);
            const origId = dlq.originalJobId.substring(0, 8);
            const movedTime = new Date(dlq.movedToDlqAt).toLocaleTimeString();
            const replayedBadge = dlq.replayed
                ? '<span class="badge badge-success">Replayed</span>'
                : '<span class="badge badge-danger">Unreplayed</span>';

            const actionBtn = dlq.replayed
                ? '-'
                : `<button class="btn btn-secondary" onclick="replaySingleDlq('${dlq.id}')">↺ Replay</button>`;

            return `
                <tr>
                    <td class="mono" title="${dlq.id}">${shortId}...</td>
                    <td class="mono" title="${dlq.originalJobId}">${origId}...</td>
                    <td><strong>${dlq.jobType}</strong></td>
                    <td>${dlq.retryCount}</td>
                    <td class="text-red" title="${escapeHtml(dlq.failureReason || '')}">${truncate(dlq.failureReason || 'Error', 40)}</td>
                    <td>${movedTime}</td>
                    <td>${replayedBadge}</td>
                    <td>${actionBtn}</td>
                </tr>
            `;
        }).join("");
    } catch (err) {
        console.error("Failed to fetch DLQ", err);
    }
}

async function handleEnqueue(e) {
    e.preventDefault();

    const jobType = document.getElementById("job-type").value;
    const priority = parseInt(document.getElementById("priority").value, 10);
    const delaySeconds = parseInt(document.getElementById("delay-sec").value, 10);
    const maxRetries = parseInt(document.getElementById("max-retries").value, 10);
    const backoffBaseSeconds = parseInt(document.getElementById("backoff-base").value, 10);
    const idempotencyKey = document.getElementById("idempotency-key").value.trim() || null;
    const payload = document.getElementById("job-payload").value.trim();

    try {
        const res = await fetch("/api/jobs", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                jobType,
                priority,
                delaySeconds,
                maxRetries,
                backoffBaseSeconds,
                idempotencyKey,
                payload
            })
        });

        if (res.ok) {
            fetchStats();
            fetchJobs();
            // Reset idempotency key for next run
            document.getElementById("idempotency-key").value = "";
        } else {
            const err = await res.json();
            alert("Failed to enqueue job: " + (err.message || res.statusText));
        }
    } catch (err) {
        alert("Network error: " + err.message);
    }
}

async function runQuickFailureDemo() {
    // Configures a simulated job that will fail 3 times and move to DLQ
    try {
        const res = await fetch("/api/jobs", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                jobType: "SIMULATED_FAILURE",
                priority: 8,
                maxRetries: 3,
                backoffBaseSeconds: 1,
                payload: JSON.stringify({ alwaysFail: true, reason: "Payment gateway connection reset" })
            })
        });
        if (res.ok) {
            fetchStats();
            fetchJobs();
        }
    } catch (err) {
        console.error("Demo failed", err);
    }
}

async function triggerClaim() {
    try {
        await fetch("/api/jobs/trigger-claim", { method: "POST" });
        fetchStats();
        fetchJobs();
    } catch (err) {
        console.error(err);
    }
}

window.replaySingleDlq = async function(id) {
    try {
        const res = await fetch(`/api/dlq/${id}/replay`, { method: "POST" });
        if (res.ok) {
            fetchStats();
            fetchJobs();
            fetchDlq();
        }
    } catch (err) {
        console.error("Replay failed", err);
    }
};

async function replayAllDlq() {
    try {
        const res = await fetch("/api/dlq/replay-all", { method: "POST" });
        if (res.ok) {
            fetchStats();
            fetchJobs();
            fetchDlq();
        }
    } catch (err) {
        console.error("Replay all failed", err);
    }
}

function truncate(str, max) {
    if (!str) return "";
    return str.length > max ? str.substring(0, max) + "..." : str;
}

function escapeHtml(str) {
    if (!str) return "";
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
