# Reliable Job Queue CLI Client (PowerShell)
param (
    [Parameter(Position=0)]
    [string]$Command = "stats",
    [Parameter(Position=1)]
    [string]$Param1 = "",
    [Parameter(Position=2)]
    [string]$Param2 = "",
    [string]$BaseUrl = "http://localhost:8080"
)

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "       RELIABLE JOB QUEUE - CLI INSPECTOR        " -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan

switch ($Command.ToLower()) {
    "stats" {
        try {
            $resp = Invoke-RestMethod -Uri "$BaseUrl/api/queue/stats" -Method Get
            Write-Host "Node Mode:           " -NoNewline; Write-Host $resp.nodeMode -ForegroundColor Green
            Write-Host "Worker ID:           " -NoNewline; Write-Host $resp.workerId -ForegroundColor Yellow
            Write-Host "Worker Active:       " -NoNewline; Write-Host "$($resp.workerActive) (concurrency: $($resp.workerConcurrency))" -ForegroundColor Yellow
            Write-Host "Total Jobs:          " -NoNewline; Write-Host $resp.totalJobs -ForegroundColor White
            Write-Host "Pending (Ready):     " -NoNewline; Write-Host $resp.pendingJobs -ForegroundColor Cyan
            Write-Host "Delayed (Scheduled): " -NoNewline; Write-Host $resp.delayedScheduledJobs -ForegroundColor Magenta
            Write-Host "In-Flight (Running): " -NoNewline; Write-Host $resp.runningJobs -ForegroundColor Yellow
            Write-Host "Completed:           " -NoNewline; Write-Host $resp.completedJobs -ForegroundColor Green
            Write-Host "Failed:              " -NoNewline; Write-Host $resp.failedJobs -ForegroundColor Red
            Write-Host "Dead-Letter Queue:   " -NoNewline; Write-Host "$($resp.deadLetterJobs) (Unreplayed: $($resp.unreplayedDlqJobs))" -ForegroundColor Red
        } catch {
            Write-Host "Error connecting to service at $BaseUrl: $_" -ForegroundColor Red
        }
    }

    "dlq" {
        try {
            $resp = Invoke-RestMethod -Uri "$BaseUrl/api/dlq?size=10" -Method Get
            if ($resp.content.Count -eq 0) {
                Write-Host "Dead Letter Queue is empty." -ForegroundColor Green
            } else {
                foreach ($item in $resp.content) {
                    Write-Host "[$($item.id.Substring(0,8))] Type: $($item.jobType), Retries: $($item.retryCount), Replayed: $($item.replayed)" -ForegroundColor Yellow
                    Write-Host "  Reason: $($item.failureReason)" -ForegroundColor Red
                    Write-Host "  Original Job ID: $($item.originalJobId)" -ForegroundColor Gray
                }
            }
        } catch {
            Write-Host "Error fetching DLQ: $_" -ForegroundColor Red
        }
    }

    "replay" {
        if ([string]::IsNullOrWhiteSpace($Param1)) {
            Write-Host "Replaying ALL dead letter jobs..." -ForegroundColor Yellow
            $resp = Invoke-RestMethod -Uri "$BaseUrl/api/dlq/replay-all" -Method Post
            Write-Host $resp.message -ForegroundColor Green
        } else {
            Write-Host "Replaying DLQ item: $Param1..." -ForegroundColor Yellow
            $resp = Invoke-RestMethod -Uri "$BaseUrl/api/dlq/$Param1/replay" -Method Post
            Write-Host "Successfully replayed as Job ID: $($resp.id)" -ForegroundColor Green
        }
    }

    "enqueue" {
        $type = if ($Param1) { $Param1 } else { "PAYMENT_PROCESS" }
        $payload = if ($Param2) { $Param2 } else { '{"amount": 149.99, "accountId": "acc_cli_demo"}' }
        $body = @{
            jobType = $type
            payload = $payload
            priority = 7
        } | ConvertTo-Json

        $resp = Invoke-RestMethod -Uri "$BaseUrl/api/jobs" -Method Post -ContentType "application/json" -Body $body
        Write-Host "Successfully enqueued Job: $($resp.id) [Type: $($resp.jobType), Status: $($resp.status)]" -ForegroundColor Green
    }

    "claim" {
        Invoke-RestMethod -Uri "$BaseUrl/api/jobs/trigger-claim" -Method Post | Out-Null
        Write-Host "Triggered worker poll & claim cycle." -ForegroundColor Green
    }

    default {
        Write-Host "Usage: .\cli.ps1 [stats | dlq | replay <id> | enqueue <type> <json> | claim]" -ForegroundColor Yellow
    }
}
Write-Host "=================================================" -ForegroundColor Cyan
