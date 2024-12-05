package io.example.application;

import static akka.Done.done;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import akka.javasdk.eventsourcedentity.EventSourcedEntityContext;
import io.example.domain.TimeSlot;

@ComponentId("timeSlot")
public class TimeSlotEntity extends EventSourcedEntity<TimeSlot.State, TimeSlot.Event> {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final String entityId;

  public TimeSlotEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public TimeSlot.State emptyState() {
    return TimeSlot.State.empty();
  }

  public Effect<Done> createTimeSlot(TimeSlot.Command.MakeTimeSlotAvailable command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);

    return effects()
        .persistAll(currentState().onCommand(command).stream().toList())
        .thenReply(newState -> done());
  }

  public Effect<Done> makeTimeSlotUnavailable(TimeSlot.Command.MakeTimeSlotUnavailable command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);

    return effects()
        .persistAll(currentState().onCommand(command).stream().toList())
        .thenReply(newState -> done());
  }

  public Effect<Done> studentRequestsTimeSlot(TimeSlot.Command.StudentRequestsTimeSlot command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);

    return effects()
        .persistAll(currentState().onCommand(command).stream().toList())
        .thenReply(newState -> done());
  }

  public Effect<Done> instructorRequestsTimeSlot(TimeSlot.Command.InstructorRequestsTimeSlot command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);

    return effects()
        .persistAll(currentState().onCommand(command).stream().toList())
        .thenReply(newState -> done());
  }

  public Effect<Done> aircraftRequestsTimeSlot(TimeSlot.Command.AircraftRequestsTimeSlot command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);

    return effects()
        .persistAll(currentState().onCommand(command).stream().toList())
        .thenReply(newState -> done());
  }

  public Effect<Done> cancelTimeSlot(TimeSlot.Command.CancelTimeSlot command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);

    return effects()
        .persistAll(currentState().onCommand(command).stream().toList())
        .thenReply(newState -> done());
  }

  public ReadOnlyEffect<TimeSlot.State> get() {
    log.info("EntityId: {}\n_State: {}", entityId, currentState());
    if (currentState().isEmpty()) {
      return effects().error("Time slot not found");
    }
    return effects().reply(currentState());
  }

  @Override
  public TimeSlot.State applyEvent(TimeSlot.Event event) {
    return switch (event) {
      case TimeSlot.Event.TimeSlotMadeAvailable e -> currentState().onEvent(e);
      case TimeSlot.Event.TimeSlotMadeUnavailable e -> currentState().onEvent(e);
      case TimeSlot.Event.TimeSlotReservationCancelled e -> currentState().onEvent(e);
      case TimeSlot.Event.StudentRequestAccepted e -> currentState().onEvent(e);
      case TimeSlot.Event.StudentRequestRejected e -> currentState().onEvent(e);
      case TimeSlot.Event.InstructorRequestAccepted e -> currentState().onEvent(e);
      case TimeSlot.Event.InstructorRequestRejected e -> currentState().onEvent(e);
      case TimeSlot.Event.AircraftRequestAccepted e -> currentState().onEvent(e);
      case TimeSlot.Event.AircraftRequestRejected e -> currentState().onEvent(e);
    };
  }
}
