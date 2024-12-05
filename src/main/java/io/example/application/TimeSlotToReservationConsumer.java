package io.example.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import io.example.domain.Reservation;
import io.example.domain.TimeSlot;

@ComponentId("timeSlot-reservation-consumer")
@Consume.FromEventSourcedEntity(TimeSlotEntity.class)
public class TimeSlotToReservationConsumer extends Consumer {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ComponentClient componentClient;

  public TimeSlotToReservationConsumer(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect onEvent(TimeSlot.Event event) {
    return switch (event) {
      case TimeSlot.Event.StudentRequestAccepted e -> onEvent(e);
      case TimeSlot.Event.StudentRequestRejected e -> onEvent(e);
      case TimeSlot.Event.InstructorRequestAccepted e -> onEvent(e);
      case TimeSlot.Event.InstructorRequestRejected e -> onEvent(e);
      case TimeSlot.Event.AircraftRequestAccepted e -> onEvent(e);
      case TimeSlot.Event.AircraftRequestRejected e -> onEvent(e);
      // Ignore other events
      default -> effects().ignore();
    };
  }

  private Effect onEvent(TimeSlot.Event.StudentRequestAccepted event) {
    log.info("Event: {}", event);
    var command = new Reservation.Command.StudentAvailable(event.reservationId());
    var reservation = componentClient.forEventSourcedEntity(event.reservationId())
        .method(ReservationEntity::studentAvailable)
        .invokeAsync(command);
    return effects().asyncDone(reservation);
  }

  private Effect onEvent(TimeSlot.Event.StudentRequestRejected event) {
    log.info("Event: {}", event);
    var command = new Reservation.Command.StudentUnavailable(event.reservationId());
    var reservation = componentClient.forEventSourcedEntity(event.reservationId())
        .method(ReservationEntity::studentUnavailable)
        .invokeAsync(command);
    return effects().asyncDone(reservation);
  }

  private Effect onEvent(TimeSlot.Event.InstructorRequestAccepted event) {
    log.info("Event: {}", event);
    var command = new Reservation.Command.InstructorAvailable(event.reservationId());
    var reservation = componentClient.forEventSourcedEntity(event.reservationId())
        .method(ReservationEntity::instructorAvailable)
        .invokeAsync(command);
    return effects().asyncDone(reservation);
  }

  private Effect onEvent(TimeSlot.Event.InstructorRequestRejected event) {
    log.info("Event: {}", event);
    var command = new Reservation.Command.InstructorUnavailable(event.reservationId());
    var reservation = componentClient.forEventSourcedEntity(event.reservationId())
        .method(ReservationEntity::instructorUnavailable)
        .invokeAsync(command);
    return effects().asyncDone(reservation);
  }

  private Effect onEvent(TimeSlot.Event.AircraftRequestAccepted event) {
    log.info("Event: {}", event);
    var command = new Reservation.Command.AircraftAvailable(event.reservationId());
    var reservation = componentClient.forEventSourcedEntity(event.reservationId())
        .method(ReservationEntity::aircraftAvailable)
        .invokeAsync(command);
    return effects().asyncDone(reservation);
  }

  private Effect onEvent(TimeSlot.Event.AircraftRequestRejected event) {
    log.info("Event: {}", event);
    var command = new Reservation.Command.AircraftUnavailable(event.reservationId());
    var reservation = componentClient.forEventSourcedEntity(event.reservationId())
        .method(ReservationEntity::aircraftUnavailable)
        .invokeAsync(command);
    return effects().asyncDone(reservation);
  }
}
