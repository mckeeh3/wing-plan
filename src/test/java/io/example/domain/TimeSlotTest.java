package io.example.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import io.example.domain.TimeSlot.ParticipantType;

class TimeSlotTest {
  @Test
  void entityIdFromParticipantIdAndTypeAndStartTime() {
    var participantId = "participant-1";
    var participantType = ParticipantType.aircraft;
    var startTime = Instant.parse("2024-03-20T10:00:00Z");
    var timeSlotId = TimeSlot.State.entityId(participantId, participantType, startTime);
    assertThat(timeSlotId).isEqualTo("2024-03-20-10-aircraft-participant-1");
  }

  @Test
  void shouldMakeTimeSlotAvailable() {
    // given
    var command = new TimeSlot.Command.MakeTimeSlotAvailable(
        "participant-1",
        ParticipantType.aircraft,
        Instant.parse("2024-03-20T10:00:00Z"));

    // when
    var state = TimeSlot.State.empty();
    var eventOpt = state.onCommand(command);

    // then
    assertThat(eventOpt).isPresent();
    var event = eventOpt.get();
    assertThat(event).isInstanceOf(TimeSlot.Event.TimeSlotMadeAvailable.class);
    var available = (TimeSlot.Event.TimeSlotMadeAvailable) event;
    var timeSlotId = TimeSlot.State.entityId(command.participantId(), command.participantType(), command.startTime());
    assertThat(available.timeSlotId()).isEqualTo(timeSlotId);
    assertThat(available.participantId()).isEqualTo(command.participantId());
    assertThat(available.participantType()).isEqualTo(command.participantType());
    assertThat(available.startTime()).isEqualTo(command.startTime());
  }

  @Test
  void shouldNotChangeStateWhenAlreadyAvailable() {
    // given
    var state = makeTimeSlotAvailable();
    var command = new TimeSlot.Command.MakeTimeSlotAvailable(
        "participant-2",
        ParticipantType.aircraft,
        Instant.parse("2024-03-20T10:00:00Z"));

    // when
    var eventOpt = state.onCommand(command);

    // then
    assertThat(eventOpt).isEmpty();
  }

  @Test
  void shouldMakeTimeSlotUnavailable() {
    // given
    var state = makeTimeSlotAvailable();
    var command = new TimeSlot.Command.MakeTimeSlotUnavailable(
        state.participantId(),
        state.participantType(),
        state.startTime());

    // when
    var eventOpt = state.onCommand(command);

    // then
    assertThat(eventOpt).isPresent();
    var event = eventOpt.get();
    assertThat(event).isInstanceOf(TimeSlot.Event.TimeSlotMadeUnavailable.class);
    var unavailable = (TimeSlot.Event.TimeSlotMadeUnavailable) event;
    var timeSlotId = TimeSlot.State.entityId(command.participantId(), command.participantType(), command.startTime());
    assertThat(unavailable.timeSlotId()).isEqualTo(timeSlotId);
  }

  @Test
  void shouldNotChangeStateWhenAlreadyUnavailable() {
    // given
    var state = makeTimeSlotAvailable();
    var command = new TimeSlot.Command.MakeTimeSlotUnavailable(
        state.participantId(),
        state.participantType(),
        state.startTime());

    // when
    state = state.onEvent(new TimeSlot.Event.TimeSlotMadeUnavailable(state.timeSlotId())); // duplicate event to simulate duplicate command
    var eventOpt = state.onCommand(command);

    // then
    assertThat(eventOpt).isEmpty();
  }

  @Test
  void makeAvailableThenMakeUnavailableThenMakeAvailable() {
    // given
    var state = makeTimeSlotAvailable();
    state = state.onEvent(new TimeSlot.Event.TimeSlotMadeUnavailable(state.timeSlotId()));
    var command = new TimeSlot.Command.MakeTimeSlotAvailable(
        state.participantId(),
        state.participantType(),
        state.startTime());

    // when
    var eventOpt = state.onCommand(command);

    // then
    assertThat(eventOpt).isPresent();
    var event = eventOpt.get();
    assertThat(event).isInstanceOf(TimeSlot.Event.TimeSlotMadeAvailable.class);
    var available = (TimeSlot.Event.TimeSlotMadeAvailable) event;
    var timeSlotId = TimeSlot.State.entityId(command.participantId(), command.participantType(), command.startTime());
    assertThat(available.timeSlotId()).isEqualTo(timeSlotId);
    assertThat(available.participantId()).isEqualTo(command.participantId());
    assertThat(available.participantType()).isEqualTo(command.participantType());
    assertThat(available.startTime()).isEqualTo(command.startTime());
  }

  @Test
  void shouldAcceptStudentRequestWhenAvailable() {
    // given
    var state = makeTimeSlotAvailable();
    var command = new TimeSlot.Command.StudentRequestsTimeSlot(
        "timeSlot-1",
        "reservation-1");

    // when
    var eventOpt = state.onCommand(command);

    // then
    assertThat(eventOpt).isPresent();
    var event = eventOpt.get();
    assertThat(event).isInstanceOf(TimeSlot.Event.StudentRequestAccepted.class);
    var accepted = (TimeSlot.Event.StudentRequestAccepted) event;
    assertThat(accepted.timeSlotId()).isEqualTo(command.timeSlotId());
    assertThat(accepted.reservationId()).isEqualTo(command.reservationId());

    var newState = state.onEvent(accepted);
    assertThat(newState.status()).isEqualTo(TimeSlot.Status.scheduled);
    assertThat(newState.reservationId()).isEqualTo(command.reservationId());
  }

  @Test
  void shouldAcceptStudentRequestWhenAlreadyScheduled() {
    // given
    var state = makeTimeSlotAvailable();
    state = state.onEvent(new TimeSlot.Event.StudentRequestAccepted("timeSlot-1", "reservation-1"));

    var command = new TimeSlot.Command.StudentRequestsTimeSlot(
        "timeSlot-1",
        "reservation-1");

    // when
    var eventOpt = state.onCommand(command);

    // then
    assertThat(eventOpt).isEmpty();
  }

  @Test
  void shouldRejectStudentRequestWhenNotAvailable() {
    // given
    var state = makeTimeSlotAvailable();
    state = state.onEvent(new TimeSlot.Event.StudentRequestAccepted("timeSlot-1", "reservation-1"));

    var command = new TimeSlot.Command.StudentRequestsTimeSlot(
        "timeSlot-1",
        "reservation-2");

    // when
    var eventOpt = state.onCommand(command);

    // then
    assertThat(eventOpt).isPresent();
    var event = eventOpt.get();
    assertThat(event).isInstanceOf(TimeSlot.Event.StudentRequestRejected.class);
    var rejected = (TimeSlot.Event.StudentRequestRejected) event;
    assertThat(rejected.timeSlotId()).isEqualTo(command.timeSlotId());
    assertThat(rejected.reservationId()).isEqualTo(command.reservationId());
  }

  @Test
  void shouldAcceptInstructorRequestWhenAvailable() {
    // given
    var state = makeTimeSlotAvailable();
    var command = new TimeSlot.Command.InstructorRequestsTimeSlot(
        "timeSlot-1",
        "reservation-1");

    // when
    var eventOpt = state.onCommand(command);

    // then
    assertThat(eventOpt).isPresent();
    var event = eventOpt.get();
    assertThat(event).isInstanceOf(TimeSlot.Event.InstructorRequestAccepted.class);
    var accepted = (TimeSlot.Event.InstructorRequestAccepted) event;
    assertThat(accepted.timeSlotId()).isEqualTo(command.timeSlotId());
    assertThat(accepted.reservationId()).isEqualTo(command.reservationId());

    var newState = state.onEvent(accepted);
    assertThat(newState.status()).isEqualTo(TimeSlot.Status.scheduled);
    assertThat(newState.reservationId()).isEqualTo(command.reservationId());
  }

  @Test
  void shouldRejectInstructorRequestWhenNotAvailable() {
    // given
    var state = makeTimeSlotAvailable();
    // First schedule it
    state = state.onEvent(new TimeSlot.Event.InstructorRequestAccepted("timeSlot-1", "reservation-1"));

    var command = new TimeSlot.Command.InstructorRequestsTimeSlot(
        "timeSlot-1",
        "reservation-2");

    // when
    var eventOpt = state.onCommand(command);

    // then
    assertThat(eventOpt).isPresent();
    var event = eventOpt.get();
    assertThat(event).isInstanceOf(TimeSlot.Event.InstructorRequestRejected.class);
    var rejected = (TimeSlot.Event.InstructorRequestRejected) event;
    assertThat(rejected.timeSlotId()).isEqualTo(command.timeSlotId());
    assertThat(rejected.reservationId()).isEqualTo(command.reservationId());
  }

  @Test
  void shouldAcceptAircraftRequestWhenAvailable() {
    // given
    var state = makeTimeSlotAvailable();
    var command = new TimeSlot.Command.AircraftRequestsTimeSlot(
        "timeSlot-1",
        "reservation-1");

    // when
    var eventOpt = state.onCommand(command);

    // then
    assertThat(eventOpt).isPresent();
    var event = eventOpt.get();
    assertThat(event).isInstanceOf(TimeSlot.Event.AircraftRequestAccepted.class);
    var accepted = (TimeSlot.Event.AircraftRequestAccepted) event;
    assertThat(accepted.timeSlotId()).isEqualTo(command.timeSlotId());
    assertThat(accepted.reservationId()).isEqualTo(command.reservationId());

    var newState = state.onEvent(accepted);
    assertThat(newState.status()).isEqualTo(TimeSlot.Status.scheduled);
    assertThat(newState.reservationId()).isEqualTo(command.reservationId());
  }

  @Test
  void shouldRejectAircraftRequestWhenNotAvailable() {
    // given
    var state = makeTimeSlotAvailable();
    // First schedule it
    state = state.onEvent(new TimeSlot.Event.AircraftRequestAccepted("timeSlot-1", "reservation-1"));

    var command = new TimeSlot.Command.AircraftRequestsTimeSlot(
        "timeSlot-1",
        "reservation-2");

    // when
    var eventOpt = state.onCommand(command);

    // then
    assertThat(eventOpt).isPresent();
    var event = eventOpt.get();
    assertThat(event).isInstanceOf(TimeSlot.Event.AircraftRequestRejected.class);
    var rejected = (TimeSlot.Event.AircraftRequestRejected) event;
    assertThat(rejected.timeSlotId()).isEqualTo(command.timeSlotId());
    assertThat(rejected.reservationId()).isEqualTo(command.reservationId());
  }

  @Test
  void shouldCancelTimeSlot() {
    // given
    var state = makeTimeSlotAvailable();
    // First schedule it
    state = state.onEvent(new TimeSlot.Event.StudentRequestAccepted("timeSlot-1", "reservation-1"));

    var command = new TimeSlot.Command.CancelTimeSlot(
        "timeSlot-1",
        "reservation-1");

    // when
    var eventOpt = state.onCommand(command);

    // then
    assertThat(eventOpt).isPresent();
    var event = eventOpt.get();
    assertThat(event).isInstanceOf(TimeSlot.Event.TimeSlotReservationCancelled.class);
    var cancelled = (TimeSlot.Event.TimeSlotReservationCancelled) event;
    assertThat(cancelled.timeSlotId()).isEqualTo(command.timeSlotId());
    assertThat(cancelled.reservationId()).isEqualTo(command.reservationId());

    var newState = state.onEvent(cancelled);
    assertThat(newState.status()).isEqualTo(TimeSlot.Status.available);
    assertThat(newState.reservationId()).isNull();
  }

  @Test
  void shouldNotCancelTimeSlotWhenNotScheduled() {
    // given
    var state = makeTimeSlotAvailable();
    var command = new TimeSlot.Command.CancelTimeSlot(
        "timeSlot-1",
        "reservation-1");

    // when
    var eventOpt = state.onCommand(command);

    // then
    assertThat(eventOpt).isEmpty();
  }

  @Test
  void shouldNotCancelTimeSlotWhenScheduledToDifferentReservation() {
    // given
    var state = makeTimeSlotAvailable();
    state = state.onEvent(new TimeSlot.Event.StudentRequestAccepted("timeSlot-1", "reservation-1"));

    var command = new TimeSlot.Command.CancelTimeSlot(
        "timeSlot-1",
        "reservation-2");

    // when
    var eventOpt = state.onCommand(command);

    // then
    assertThat(eventOpt).isEmpty();

    // then verify the time slot is still scheduled
    assertThat(state.status()).isEqualTo(TimeSlot.Status.scheduled);
    assertThat(state.reservationId()).isEqualTo("reservation-1");
  }

  // Helper method
  private TimeSlot.State makeTimeSlotAvailable() {
    var command = new TimeSlot.Command.MakeTimeSlotAvailable(
        "participant-1",
        ParticipantType.aircraft,
        Instant.parse("2024-03-20T10:00:00Z"));
    var state = TimeSlot.State.empty();
    var event = (TimeSlot.Event.TimeSlotMadeAvailable) state.onCommand(command).get();
    return state.onEvent(event);
  }
}
