#!/bin/bash

# Check for required parameters
if [ "$#" -lt 3 ]; then
    echo "Usage: $0 [host] time_slot_id student_id"
    echo "  host        - Optional. Server host (default: localhost:9000)"
    echo "  time_slot_id - Required. Unique time slot identifier"
    echo "  student_id   - Required. Student identifier"
    echo "  start_offset - Required: Time slot time offset"
    exit 1
fi

# Parse parameters based on argument count
if [ "$#" -eq 3 ]; then
    # No host provided, use default
    host="localhost:9000"
    timeSlotId="$1"
    studentId="$2"
    startTime="$3"
    urlScheme="http"
else
    # Host provided
    host="$1"
    timeSlotId="$2"
    studentId="$3"
    urlScheme="https"
fi

# Create JSON body
json_body=$(cat <<EOF
{
  "timeSlotId": "${timeSlotId}",
  "participantId": "${studentId}",
  "participantType": "student",
  "startTime": "$(date -u -v+${startTime}H +%Y-%m-%dT%H:%M:00Z)"
}
EOF
)

curl -X POST "${urlScheme}://${host}/flight/create-time-slot" \
  -H "Content-Type: application/json" \
  -d "$json_body"

echo # Add newline after curl output
