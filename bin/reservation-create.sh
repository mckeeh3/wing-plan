#!/bin/bash

# Check for required parameters
if [ "$#" -lt 4 ]; then
    echo "Usage: $0 [host] reservation_id student_time_slot_id instructor_time_slot_id aircraft_time_slot_id"
    echo "  host                  - Optional. Server host (default: localhost:9000)"
    echo "  reservation_id        - Required. Unique reservation identifier"
    echo "  student_time_slot_id  - Required. Student time slot identifier"
    echo "  instructor_time_slot_id - Required. Instructor time slot identifier" 
    echo "  aircraft_time_slot_id - Required. Aircraft time slot identifier"
    exit 1
fi

# Parse parameters based on argument count
if [ "$#" -eq 4 ]; then
    # No host provided, use default
    host="localhost:9000"
    reservationId="$1"
    studentTimeSlotId="$2"
    instructorTimeSlotId="$3"
    aircraftTimeSlotId="$4"
    urlScheme="http"
else
    # Host provided
    host="$1"
    reservationId="$2"
    studentTimeSlotId="$3"
    instructorTimeSlotId="$4"
    aircraftTimeSlotId="$5"
    urlScheme="https"
fi

# Create JSON body
json_body=$(cat <<EOF
{
  "reservationId": "${reservationId}",
  "studentTimeSlotId": "${studentTimeSlotId}",
  "instructorTimeSlotId": "${instructorTimeSlotId}",
  "aircraftTimeSlotId": "${aircraftTimeSlotId}",
  "reservationTime": "$(date -u +%Y-%m-%dT%H:%M:00Z)"
}
EOF
)

curl -X POST "${urlScheme}://${host}/flight/reservation" \
  -H "Content-Type: application/json" \
  -d "$json_body"

echo # Add newline after curl output
