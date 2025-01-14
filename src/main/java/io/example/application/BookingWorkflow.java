package io.example.application;

import static akka.Done.done;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

    var reservationTime = request.reservationTime().truncatedTo(ChronoUnit.HOURS);
    var command = new TimeSlotView.ByParticipantAndTimeRange(
        request.studentId(),
        "student",
        reservationTime,
        reservationTime.plus(Duration.ofHours(1)));
    return effects()
        .updateState(Booking.State.initialState(request.studentId(), request.reservationTime()))
        .transitionTo("check-if-student-is-available", command)
        .thenReply(done());
  }

  public ReadOnlyEffect<Booking.State> get() {
    return effects().reply(currentState());
  }

  @Override
  public WorkflowDef<Booking.State> definition() {
    var checkIfStudentIsAvailable = step("check-if-student-is-available")
        .asyncCall(TimeSlotView.ByParticipantAndTimeRange.class,
            command -> componentClient.forView()
                .method(TimeSlotView::getTimeSlotsByParticipantAndTimeRange)
                .invokeAsync(command))
        .andThen(TimeSlotView.TimeSlots.class, queryResponse -> {
          if (queryResponse.timeSlots().isEmpty() || !queryResponse.timeSlots().get(0).status().equals(TimeSlot.Status.available.name())) {
            return effects()
                .updateState(currentState().withStatus(Booking.Status.cancelledStudentNotAvailable))
                .end();
          }
          var studentTimeSlotId = queryResponse.timeSlots().get(0).timeSlotId();
          var startTime = currentState().reservationTime();
          var endTime = startTime.plus(Duration.ofHours(1));
          var nextCommand = new TimeSlotView.ByParticipantTypeAndTimeRange(TimeSlot.ParticipantType.instructor.name(), startTime, endTime);
          return effects()
              .updateState(currentState().withStudentTimeSlot(studentTimeSlotId))
              .transitionTo("find-available-instructor", nextCommand);
        });

    var findAvailableInstructor = step("find-available-instructor")
        .asyncCall(TimeSlotView.ByParticipantTypeAndTimeRange.class,
            command -> componentClient.forView()
                .method(TimeSlotView::getTimeSlotsByParticipantTypeAndTimeRange)
                .invokeAsync(command))
        .andThen(TimeSlotView.TimeSlots.class, queryResponse -> {
          if (queryResponse.timeSlots().isEmpty() || !queryResponse.timeSlots().get(0).status().equals(TimeSlot.Status.available.name())) {
            return effects()
                .updateState(currentState().withStatus(Booking.Status.cancelledInstructorNotAvailable))
                .end();
          }
          var instructorTimeSlotId = queryResponse.timeSlots().get(0).timeSlotId();
          var instructorId = queryResponse.timeSlots().get(0).participantId();
          var startTime = currentState().reservationTime();
          var endTime = startTime.plus(Duration.ofHours(1));
          var nextCommand = new TimeSlotView.ByParticipantTypeAndTimeRange(TimeSlot.ParticipantType.aircraft.name(), startTime, endTime);
          return effects()
              .updateState(currentState().withInstructor(instructorId, instructorTimeSlotId))
              .transitionTo("find-available-aircraft", nextCommand);
        });

    var findAvailableAircraft = step("find-available-aircraft")
        .asyncCall(TimeSlotView.ByParticipantTypeAndTimeRange.class,
            command -> componentClient.forView()
                .method(TimeSlotView::getTimeSlotsByParticipantTypeAndTimeRange)
                .invokeAsync(command))
        .andThen(TimeSlotView.TimeSlots.class, queryResponse -> {
          if (queryResponse.timeSlots().isEmpty() || !queryResponse.timeSlots().get(0).status().equals(TimeSlot.Status.available.name())) {
            return effects()
                .updateState(currentState().withStatus(Booking.Status.cancelledAircraftNotAvailable))
                .end();
          }
          var aircraftTimeSlotId = queryResponse.timeSlots().get(0).timeSlotId();
          var aircraftId = queryResponse.timeSlots().get(0).participantId();
          var reservationId = Reservation.generateReservationId();
          var nextCommand = new Reservation.Command.CreateReservation(
              reservationId,
              currentState().studentId(),
              currentState().studentTimeSlotId(),
              currentState().instructorId(),
              currentState().instructorTimeSlotId(),
              aircraftId,
              aircraftTimeSlotId,
              currentState().reservationTime());
          return effects()
              .updateState(currentState().withAircraftAndReservationId(aircraftId, aircraftTimeSlotId, reservationId))
              .transitionTo("create-reservation", nextCommand);
        });

    var createReservation = step("create-reservation")
        .asyncCall(Reservation.Command.CreateReservation.class,
            command -> componentClient.forEventSourcedEntity(command.reservationId())
                .method(ReservationEntity::createReservation)
                .invokeAsync(command))
        .andThen(Done.class, __ -> {
          effects().updateState(currentState().withStatus(Booking.Status.reservationRequested));
          return effects().end();
        });

    return workflow()
        .addStep(checkIfStudentIsAvailable)
        .addStep(findAvailableInstructor)
        .addStep(findAvailableAircraft)
        .addStep(createReservation);
  }
}
