package io.example.application;

import static akka.Done.done;

import java.time.Duration;
import java.time.Instant;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.workflow.Workflow;
import io.example.domain.Booking;
import io.example.domain.Reservation;
import io.example.domain.TimeSlot;

@ComponentId("booking")
public class BookingWorkflow extends Workflow<Booking.State> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BookingWorkflow.class);
  private final ComponentClient componentClient;

  public BookingWorkflow(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public record BookingRequest(String reservationId, String studentId, Instant reservationTime) {}

  public Effect<Done> startBooking(BookingRequest request) {
    log.info("{}", request);
    var command = new TimeSlotView.ByStudentIdAndTimeRange(
        request.studentId(),
        request.reservationTime(),
        request.reservationTime().plus(Duration.ofHours(1)));
    return effects()
        .updateState(Booking.State.initialState(request.studentId(), request.reservationTime()))
        .transitionTo("check-if-student-is-available", command)
        .thenReply(done());
  }

  public Effect<Done> confirmOrCancelBooking(Reservation.Status status) {
    if (currentState().status() == Booking.Status.pending) {
      if (status == Reservation.Status.confirmed) {
        return effects()
            .updateState(currentState().withStatus(Booking.Status.confirmed))
            .end()
            .thenReply(done());
      } else {
        return effects()
            .updateState(currentState().withStatus(Booking.Status.reservationUnavailable))
            .end()
            .thenReply(done());
      }
    }
    return effects().error("Booking is not pending");
  }

  public ReadOnlyEffect<Booking.State> get() {
    return effects().reply(currentState());
  }

  @Override
  public WorkflowDef<Booking.State> definition() {
    var checkIfStudentIsAvailable = step("check-if-student-is-available")
        .asyncCall(TimeSlotView.ByStudentIdAndTimeRange.class, command -> componentClient.forView()
            .method(TimeSlotView::getTimeSlotsByStudentIdAndTimeRange)
            .invokeAsync(command))
        .andThen(TimeSlotView.TimeSlots.class, queryResponse -> {
          if (queryResponse.timeSlots().isEmpty()) {
            return effects()
                .updateState(currentState().withStatus(Booking.Status.cancelledStudentNotAvailable))
                .end();
          }
          var studentTimeSlotId = queryResponse.timeSlots().get(0).timeSlotId();
          var startTime = currentState().reservationTime();
          var endTime = startTime.plus(Duration.ofHours(1));
          var nextCommand = new TimeSlotView.ByTypeAndTimeRange(TimeSlot.ParticipantType.instructor.name(), startTime, endTime);
          return effects()
              .updateState(currentState().withStudentTimeSlot(studentTimeSlotId))
              .transitionTo("find-available-instructor", nextCommand);
        });

    var findAvailableInstructor = step("find-available-instructor")
        .asyncCall(TimeSlotView.ByTypeAndTimeRange.class, command -> componentClient.forView()
            .method(TimeSlotView::getTimeSlotsByParticipantTypeAndTimeRange)
            .invokeAsync(command))
        .andThen(TimeSlotView.TimeSlots.class, queryResponse -> {
          if (queryResponse.timeSlots().isEmpty()) {
            return effects()
                .updateState(currentState().withStatus(Booking.Status.cancelledInstructorNotAvailable))
                .end();
          }
          var instructorTimeSlotId = queryResponse.timeSlots().get(0).timeSlotId();
          var instructorId = queryResponse.timeSlots().get(0).participantId();
          var startTime = currentState().reservationTime();
          var endTime = startTime.plus(Duration.ofHours(1));
          var nextCommand = new TimeSlotView.ByTypeAndTimeRange(TimeSlot.ParticipantType.aircraft.name(), startTime, endTime);
          return effects()
              .updateState(currentState().withInstructor(instructorId, instructorTimeSlotId))
              .transitionTo("find-available-aircraft", nextCommand);
        });

    var findAvailableAircraft = step("find-available-aircraft")
        .asyncCall(TimeSlotView.ByTypeAndTimeRange.class, command -> componentClient.forView()
            .method(TimeSlotView::getTimeSlotsByParticipantTypeAndTimeRange)
            .invokeAsync(command))
        .andThen(TimeSlotView.TimeSlots.class, queryResponse -> {
          if (queryResponse.timeSlots().isEmpty()) {
            return effects()
                .updateState(currentState().withStatus(Booking.Status.cancelledAircraftNotAvailable))
                .end();
          }
          var aircraftTimeSlotId = queryResponse.timeSlots().get(0).timeSlotId();
          var aircraftId = queryResponse.timeSlots().get(0).participantId();
          var nextCommand = new Reservation.Command.CreateReservation(
              Reservation.generateReservationId(),
              currentState().studentId(),
              currentState().studentTimeSlotId(),
              currentState().instructorId(),
              currentState().instructorTimeSlotId(),
              aircraftId,
              aircraftTimeSlotId,
              currentState().reservationTime());
          return effects()
              .updateState(currentState().withAircraft(aircraftId, aircraftTimeSlotId))
              .transitionTo("create-reservation", nextCommand);
        });

    var createReservation = step("create-reservation")
        .asyncCall(Reservation.Command.CreateReservation.class, command -> componentClient.forEventSourcedEntity(command.reservationId())
            .method(ReservationEntity::createReservation)
            .invokeAsync(command))
        .andThen(Done.class, __ -> effects().pause());

    return workflow()
        .addStep(checkIfStudentIsAvailable)
        .addStep(findAvailableInstructor)
        .addStep(findAvailableAircraft)
        .addStep(createReservation);
  }
}
