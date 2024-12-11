package io.example.api;

import java.util.concurrent.CompletionStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.Done;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.annotations.http.Put;
import akka.javasdk.client.ComponentClient;
import io.example.application.BookingWorkflow;
import io.example.application.ReservationEntity;
import io.example.application.TimeSlotEntity;
import io.example.application.TimeSlotView;
import io.example.domain.Reservation;
import io.example.domain.TimeSlot;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/flight")
public class ReservationEndpoint {
  private final Logger log = LoggerFactory.getLogger(ReservationEndpoint.class);

  private final ComponentClient componentClient;

  public ReservationEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Post("/booking")
  public CompletionStage<Done> startBooking(BookingWorkflow.BookingRequest request) {
    log.info("{}", request);
    return componentClient.forWorkflow(request.reservationId())
        .method(BookingWorkflow::startBooking)
        .invokeAsync(request);
  }

  @Post("/reservation")
  public CompletionStage<Done> create(Reservation.Command.CreateReservation command) {
    log.info("{}", command);
    return componentClient.forEventSourcedEntity(command.reservationId())
        .method(ReservationEntity::createReservation)
        .invokeAsync(command);
  }

  @Put("/reservation-cancel")
  public CompletionStage<Done> cancel(Reservation.Command.CancelReservation command) {
    log.info("{}", command);
    return componentClient.forEventSourcedEntity(command.reservationId())
        .method(ReservationEntity::cancelReservation)
        .invokeAsync(command);
  }

  @Get("/reservation/{entityId}")
  public CompletionStage<Reservation.State> get(String entityId) {
    return componentClient.forEventSourcedEntity(entityId)
        .method(ReservationEntity::get)
        .invokeAsync();
  }

  @Post("/make-time-slot-available")
  public CompletionStage<Done> createTimeSlot(TimeSlot.Command.MakeTimeSlotAvailable command) {
    log.info("{}", command);
    return componentClient.forEventSourcedEntity(command.timeSlotId())
        .method(TimeSlotEntity::createTimeSlot)
        .invokeAsync(command);
  }

  @Put("/make-time-slot-unavailable")
  public CompletionStage<Done> makeTimeSlotUnavailable(TimeSlot.Command.MakeTimeSlotUnavailable command) {
    log.info("{}", command);
    return componentClient.forEventSourcedEntity(command.timeSlotId())
        .method(TimeSlotEntity::makeTimeSlotUnavailable)
        .invokeAsync(command);
  }

  @Get("/time-slot/{entityId}")
  public CompletionStage<TimeSlot.State> getTimeSlot(String entityId) {
    return componentClient.forEventSourcedEntity(entityId)
        .method(TimeSlotEntity::get)
        .invokeAsync();
  }

  @Get("/time-slot-view-all")
  public CompletionStage<TimeSlotView.TimeSlots> getAllTimeSlots() {
    return componentClient.forView()
        .method(TimeSlotView::getAllTimeSlots)
        .invokeAsync();
  }

  @Post("/time-slot-view-by-type-and-time-range")
  public CompletionStage<TimeSlotView.TimeSlots> getTimeSlotByTypeAndTimeRange(TimeSlotView.ByParticipantTypeAndTimeRange command) {
    return componentClient.forView()
        .method(TimeSlotView::getTimeSlotsByParticipantTypeAndTimeRange)
        .invokeAsync(command);
  }

  @Post("/time-slot-view-by-participant-and-time-range")
  public CompletionStage<TimeSlotView.TimeSlots> getTimeSlotByParticipantAndTimeRange(TimeSlotView.ByParticipantAndTimeRange command) {
    return componentClient.forView()
        .method(TimeSlotView::getTimeSlotsByParticipantAndTimeRange)
        .invokeAsync(command);
  }
}
