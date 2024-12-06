package io.example.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import io.example.domain.Reservation;
import io.example.domain.TimeSlot;

@ComponentId("reservation-timeSlots-consumer")
@Consume.FromEventSourcedEntity(ReservationEntity.class)
public class ReservationToTimeSlotConsumer extends Consumer {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ComponentClient componentClient;

  public ReservationToTimeSlotConsumer(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect onEvent(Reservation.Event event) {
    return switch (event) {
      case Reservation.Event.StudentWantsTimeSlot e -> onEvent(e);
      case Reservation.Event.InstructorWantsTimeSlot e -> onEvent(e);
      case Reservation.Event.AircraftWantsTimeSlot e -> onEvent(e);
      case Reservation.Event.CancelledStudentReservation e -> onEvent(e);
      case Reservation.Event.CancelledInstructorReservation e -> onEvent(e);
      case Reservation.Event.CancelledAircraftReservation e -> onEvent(e);
      // Ignore other events
      default -> effects().ignore();
    };
  }

  private Effect onEvent(Reservation.Event.StudentWantsTimeSlot event) {
    log.info("Event: {}", event);
    var command = new TimeSlot.Command.StudentRequestsTimeSlot(
        event.timeSlotId(),
        event.reservationId());
    var timeSlot = componentClient.forEventSourcedEntity(event.timeSlotId())
        .method(TimeSlotEntity::studentRequestsTimeSlot)
        .invokeAsync(command);
    return effects().asyncDone(timeSlot);
  }

  private Effect onEvent(Reservation.Event.InstructorWantsTimeSlot event) {
    log.info("Event: {}", event);
    var command = new TimeSlot.Command.InstructorRequestsTimeSlot(
        event.timeSlotId(),
        event.reservationId());
    var timeSlot = componentClient.forEventSourcedEntity(event.timeSlotId())
        .method(TimeSlotEntity::instructorRequestsTimeSlot)
        .invokeAsync(command);
    return effects().asyncDone(timeSlot);
  }

  private Effect onEvent(Reservation.Event.AircraftWantsTimeSlot event) {
    log.info("Event: {}", event);
    var command = new TimeSlot.Command.AircraftRequestsTimeSlot(
        event.timeSlotId(),
        event.reservationId());
    var timeSlot = componentClient.forEventSourcedEntity(event.timeSlotId())
        .method(TimeSlotEntity::aircraftRequestsTimeSlot)
        .invokeAsync(command);
    return effects().asyncDone(timeSlot);
  }

  private Effect onEvent(Reservation.Event.CancelledStudentReservation event) {
    log.info("Event: {}", event);
    var command = new TimeSlot.Command.CancelTimeSlot(
        event.studentTimeSlotId(),
        event.reservationId());
    var timeSlot = componentClient.forEventSourcedEntity(event.studentTimeSlotId())
        .method(TimeSlotEntity::cancelTimeSlot)
        .invokeAsync(command);
    return effects().asyncDone(timeSlot);
  }

  private Effect onEvent(Reservation.Event.CancelledInstructorReservation event) {
    log.info("Event: {}", event);
    var command = new TimeSlot.Command.CancelTimeSlot(
        event.instructorTimeSlotId(),
        event.reservationId());
    var timeSlot = componentClient.forEventSourcedEntity(event.instructorTimeSlotId())
        .method(TimeSlotEntity::cancelTimeSlot)
        .invokeAsync(command);
    return effects().asyncDone(timeSlot);
  }

  private Effect onEvent(Reservation.Event.CancelledAircraftReservation event) {
    log.info("Event: {}", event);
    var command = new TimeSlot.Command.CancelTimeSlot(
        event.aircraftTimeSlotId(),
        event.reservationId());
    var timeSlot = componentClient.forEventSourcedEntity(event.aircraftTimeSlotId())
        .method(TimeSlotEntity::cancelTimeSlot)
        .invokeAsync(command);
    return effects().asyncDone(timeSlot);
  }
}
