package io.example.application;

import static akka.Done.done;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import akka.javasdk.eventsourcedentity.EventSourcedEntityContext;
import io.example.domain.Reservation;

@ComponentId("reservation")
public class ReservationEntity extends EventSourcedEntity<Reservation.State, Reservation.Event> {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final String entityId;

  public ReservationEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public Reservation.State emptyState() {
    return Reservation.State.empty();
  }

  public Effect<Done> createReservation(Reservation.Command.CreateReservation command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);

    return effects()
        .persistAll(currentState().onCommand(command))
        .thenReply(newState -> done());
  }

  public Effect<Done> studentAvailable(Reservation.Command.StudentAvailable command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);

    return effects()
        .persistAll(currentState().onCommand(command))
        .thenReply(newState -> done());
  }

  public Effect<Done> studentUnavailable(Reservation.Command.StudentUnavailable command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);

    return effects()
        .persistAll(currentState().onCommand(command))
        .thenReply(newState -> done());
  }

  public Effect<Done> instructorAvailable(Reservation.Command.InstructorAvailable command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);

    return effects()
        .persistAll(currentState().onCommand(command))
        .thenReply(newState -> done());
  }

  public Effect<Done> instructorUnavailable(Reservation.Command.InstructorUnavailable command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);

    return effects()
        .persistAll(currentState().onCommand(command))
        .thenReply(newState -> done());
  }

  public Effect<Done> aircraftAvailable(Reservation.Command.AircraftAvailable command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);

    return effects()
        .persistAll(currentState().onCommand(command))
        .thenReply(newState -> done());
  }

  public Effect<Done> aircraftUnavailable(Reservation.Command.AircraftUnavailable command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);

    return effects()
        .persistAll(currentState().onCommand(command))
        .thenReply(newState -> done());
  }

  public Effect<Done> cancelReservation(Reservation.Command.CancelReservation command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);

    return effects()
        .persistAll(currentState().onCommand(command))
        .thenReply(newState -> done());
  }

  public ReadOnlyEffect<Reservation.State> get() {
    if (currentState().isEmpty()) {
      return effects().error("Reservation not found");
    }
    return effects().reply(currentState());
  }

  @Override
  public Reservation.State applyEvent(Reservation.Event event) {
    return switch (event) {
      case Reservation.Event.ReservationCreated e -> currentState().onEvent(e);
      case Reservation.Event.StudentWantsTimeSlot e -> currentState().onEvent(e);
      case Reservation.Event.StudentAvailable e -> currentState().onEvent(e);
      case Reservation.Event.StudentUnavailable e -> currentState().onEvent(e);
      case Reservation.Event.InstructorWantsTimeSlot e -> currentState().onEvent(e);
      case Reservation.Event.InstructorAvailable e -> currentState().onEvent(e);
      case Reservation.Event.InstructorUnavailable e -> currentState().onEvent(e);
      case Reservation.Event.AircraftWantsTimeSlot e -> currentState().onEvent(e);
      case Reservation.Event.AircraftAvailable e -> currentState().onEvent(e);
      case Reservation.Event.AircraftUnavailable e -> currentState().onEvent(e);
      case Reservation.Event.ReservationConfirmed e -> currentState().onEvent(e);
      case Reservation.Event.ReservationCancelled e -> currentState().onEvent(e);
      case Reservation.Event.CancelledStudentReservation e -> currentState().onEvent(e);
      case Reservation.Event.CancelledInstructorReservation e -> currentState().onEvent(e);
      case Reservation.Event.CancelledAircraftReservation e -> currentState().onEvent(e);
    };
  }
}
