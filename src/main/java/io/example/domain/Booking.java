package io.example.domain;

import java.time.Instant;

public interface Booking {
  public enum Status {
    pending,
    confirmed,
    cancelledStudentNotAvailable,
    cancelledInstructorNotAvailable,
    cancelledAircraftNotAvailable,
    reservationUnavailable
  }

  record State(
      String studentId,
      String studentTimeSlotId,
      String instructorId,
      String instructorTimeSlotId,
      String aircraftId,
      String aircraftTimeSlotId,
      Instant reservationTime,
      Status status) {
    public static State initialState(String studentId, Instant reservationTime) {
      return new State(studentId, null, null, null, null, null, reservationTime, Status.pending);
    }

    public boolean isEmpty() {
      return studentId == null;
    }

    public State withStudentTimeSlot(String newStudentTimeSlotId) {
      return new State(
          studentId,
          newStudentTimeSlotId,
          instructorId,
          instructorTimeSlotId,
          aircraftId,
          aircraftTimeSlotId,
          reservationTime,
          status);
    }

    public State withInstructor(String newInstructorId, String newInstructorTimeSlotId) {
      return new State(
          studentId,
          studentTimeSlotId,
          newInstructorId,
          newInstructorTimeSlotId,
          aircraftId,
          aircraftTimeSlotId,
          reservationTime,
          status);
    }

    public State withAircraft(String newAircraftId, String newAircraftTimeSlotId) {
      return new State(
          studentId,
          studentTimeSlotId,
          instructorId,
          instructorTimeSlotId,
          newAircraftId,
          newAircraftTimeSlotId,
          reservationTime,
          status);
    }

    public State withStatus(Status newStatus) {
      return new State(
          studentId,
          studentTimeSlotId,
          instructorId,
          instructorTimeSlotId,
          aircraftId,
          aircraftTimeSlotId,
          reservationTime,
          newStatus);
    }
  }
}
