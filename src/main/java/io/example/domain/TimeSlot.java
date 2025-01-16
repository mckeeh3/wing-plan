package io.example.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import akka.javasdk.annotations.TypeName;

public interface TimeSlot {
  public enum ParticipantType {
    student,
    instructor,
    aircraft
  }

  public enum Status {
    available, // Participant has declared availability for this time slot
    unavailable, // Participant has declared they are not available
    scheduled // Participant is confirmed for a reservation in this time slot
  }

  public record State(
      String timeSlotId,
      String participantId, // The ID of the student, instructor, or aircraft
      ParticipantType participantType, // Type of participant (student, instructor, aircraft)
      Instant startTime, // Start time of the time slot slot
      Status status, // Current status of this time slot
      String reservationId) {

    public static State empty() {
      return new State(null, null, null, Instant.EPOCH, Status.available, null);
    }

    public boolean isEmpty() {
      return timeSlotId == null;
    }

    public Optional<Event> onCommand(Command.MakeTimeSlotAvailable command) {
      if (isEmpty() || status == Status.unavailable) {
        Instant roundedTime = command.startTime.plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.HOURS);
        var timeSlotId = TimeSlot.State.entityId(command.participantId, command.participantType, command.startTime);
        return Optional.of(new Event.TimeSlotMadeAvailable(
            timeSlotId,
            command.participantId,
            command.participantType,
            roundedTime));
      }
      return Optional.empty();
    }

    public Optional<Event> onCommand(Command.MakeTimeSlotUnavailable command) {
      if (!isEmpty() && status == Status.available) {
        var timeSlotId = TimeSlot.State.entityId(command.participantId, command.participantType, command.startTime);
        return Optional.of(new Event.TimeSlotMadeUnavailable(timeSlotId));
      }
      return Optional.empty();
    }

    public Optional<Event> onCommand(Command.CancelTimeSlot command) {
      if (!isEmpty() && status == Status.scheduled
          && command.reservationId.equals(reservationId)) {
        return Optional.of(new Event.TimeSlotReservationCancelled(
            command.timeSlotId,
            command.reservationId));
      }
      return Optional.empty();
    }

    public Optional<Event> onCommand(Command.StudentRequestsTimeSlot command) {
      if (!isEmpty() && status == Status.available) {
        return Optional.of(new Event.StudentRequestAccepted(
            command.timeSlotId,
            command.reservationId));
      } else if (status == Status.scheduled && command.reservationId.equals(reservationId)) {
        return Optional.empty(); // Idempotent case - already scheduled with same reservation
      }
      return Optional.of(new Event.StudentRequestRejected(
          command.timeSlotId,
          command.reservationId));
    }

    public Optional<Event> onCommand(Command.InstructorRequestsTimeSlot command) {
      if (!isEmpty() && status == Status.available) {
        return Optional.of(new Event.InstructorRequestAccepted(
            command.timeSlotId,
            command.reservationId));
      } else if (status == Status.scheduled && command.reservationId.equals(reservationId)) {
        return Optional.empty(); // Idempotent case
      }
      return Optional.of(new Event.InstructorRequestRejected(
          command.timeSlotId,
          command.reservationId));
    }

    public Optional<Event> onCommand(Command.AircraftRequestsTimeSlot command) {
      if (!isEmpty() && status == Status.available) {
        return Optional.of(new Event.AircraftRequestAccepted(
            command.timeSlotId,
            command.reservationId));
      } else if (status == Status.scheduled && command.reservationId.equals(reservationId)) {
        return Optional.empty(); // Idempotent case
      }
      return Optional.of(new Event.AircraftRequestRejected(
          command.timeSlotId,
          command.reservationId));
    }

    public State onEvent(Event.TimeSlotMadeAvailable event) {
      var timeSlotId = entityId(event.participantId, event.participantType, event.startTime);
      return new State(
          timeSlotId.toString(),
          event.participantId,
          event.participantType,
          event.startTime,
          Status.available,
          null);
    }

    public State onEvent(Event.TimeSlotMadeUnavailable event) {
      return new State(
          timeSlotId,
          participantId,
          participantType,
          startTime,
          Status.unavailable,
          null);
    }

    public State onEvent(Event.TimeSlotReservationCancelled event) {
      return new State(
          timeSlotId,
          participantId,
          participantType,
          startTime,
          Status.available,
          null);
    }

    public State onEvent(Event.StudentRequestAccepted event) {
      return new State(
          timeSlotId,
          participantId,
          participantType,
          startTime,
          Status.scheduled,
          event.reservationId);
    }

    public State onEvent(Event.StudentRequestRejected event) {
      return this;
    }

    public State onEvent(Event.InstructorRequestAccepted event) {
      return new State(
          timeSlotId,
          participantId,
          participantType,
          startTime,
          Status.scheduled,
          event.reservationId);
    }

    public State onEvent(Event.InstructorRequestRejected event) {
      return this;
    }

    public State onEvent(Event.AircraftRequestAccepted event) {
      return new State(
          timeSlotId,
          participantId,
          participantType,
          startTime,
          Status.scheduled,
          event.reservationId);
    }

    public State onEvent(Event.AircraftRequestRejected event) {
      return this;
    }

    public static String entityId(String participantId, ParticipantType participantType, Instant startTime) {
      var roundedTime = startTime.plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.HOURS);
      var year = roundedTime.atZone(ZoneOffset.UTC).getYear();
      var month = roundedTime.atZone(ZoneOffset.UTC).getMonthValue();
      var day = roundedTime.atZone(ZoneOffset.UTC).getDayOfMonth();
      var hour = roundedTime.atZone(ZoneOffset.UTC).getHour();
      return "%d-%02d-%02d-%02d-%s-%s".formatted(year, month, day, hour, participantType.name(), participantId);
    }
  }

  public sealed interface Command {
    record MakeTimeSlotAvailable(
        String participantId,
        ParticipantType participantType,
        Instant startTime) implements Command {}

    record MakeTimeSlotUnavailable(
        String participantId,
        ParticipantType participantType,
        Instant startTime) implements Command {}

    record CancelTimeSlot(
        String timeSlotId,
        String reservationId) implements Command {}

    record StudentRequestsTimeSlot(
        String timeSlotId,
        String reservationId) implements Command {}

    record InstructorRequestsTimeSlot(
        String timeSlotId,
        String reservationId) implements Command {}

    record AircraftRequestsTimeSlot(
        String timeSlotId,
        String reservationId) implements Command {}
  }

  public sealed interface Event {
    @TypeName("TimeSlotMadeAvailable")
    record TimeSlotMadeAvailable(
        String timeSlotId,
        String participantId,
        ParticipantType participantType,
        Instant startTime) implements Event {}

    @TypeName("TimeSlotMadeUnavailable")
    record TimeSlotMadeUnavailable(
        String timeSlotId) implements Event {}

    @TypeName("StudentRequestAccepted")
    record StudentRequestAccepted(
        String timeSlotId,
        String reservationId) implements Event {}

    @TypeName("StudentRequestRejected")
    record StudentRequestRejected(
        String timeSlotId,
        String reservationId) implements Event {}

    @TypeName("InstructorRequestAccepted")
    record InstructorRequestAccepted(
        String timeSlotId,
        String reservationId) implements Event {}

    @TypeName("InstructorRequestRejected")
    record InstructorRequestRejected(
        String timeSlotId,
        String reservationId) implements Event {}

    @TypeName("AircraftRequestAccepted")
    record AircraftRequestAccepted(
        String timeSlotId,
        String reservationId) implements Event {}

    @TypeName("AircraftRequestRejected")
    record AircraftRequestRejected(
        String timeSlotId,
        String reservationId) implements Event {}

    @TypeName("TimeSlotReservationCancelled")
    record TimeSlotReservationCancelled(
        String timeSlotId,
        String reservationId) implements Event {}
  }
}
