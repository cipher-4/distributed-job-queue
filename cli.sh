#!/usr/bin/env bash
# Reliable Job Queue CLI Client (Bash)
BASE_URL=${BASE_URL:-"http://localhost:8080"}
CMD=${1:-"stats"}

echo "================================================="
echo "       RELIABLE JOB QUEUE - CLI INSPECTOR        "
echo "================================================="

case "$CMD" in
    stats)
        curl -s "$BASE_URL/api/queue/stats" | python3 -m json.tool || curl -s "$BASE_URL/api/queue/stats"
        ;;
    dlq)
        curl -s "$BASE_URL/api/dlq?size=10" | python3 -m json.tool || curl -s "$BASE_URL/api/dlq?size=10"
        ;;
    replay)
        if [ -z "$2" ]; then
            echo "Replaying ALL failed jobs..."
            curl -s -X POST "$BASE_URL/api/dlq/replay-all"
        else
            echo "Replaying DLQ item: $2..."
            curl -s -X POST "$BASE_URL/api/dlq/$2/replay"
        fi
        echo ""
        ;;
    enqueue)
        TYPE=${2:-"PAYMENT_PROCESS"}
        PAYLOAD=${3:-'{"amount": 149.99, "accountId": "acc_bash_demo"}'}
        curl -s -X POST "$BASE_URL/api/jobs" \
             -H "Content-Type: application/json" \
             -d "{\"jobType\": \"$TYPE\", \"payload\": \"$PAYLOAD\", \"priority\": 7}"
        echo ""
        ;;
    claim)
        curl -s -X POST "$BASE_URL/api/jobs/trigger-claim"
        echo "Triggered worker claim cycle."
        ;;
    *)
        echo "Usage: ./cli.sh [stats | dlq | replay <id> | enqueue <type> <json> | claim]"
        ;;
esac
echo "================================================="
