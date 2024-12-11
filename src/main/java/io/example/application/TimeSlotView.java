package io.example.application;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import io.example.domain.TimeSlot;

@ComponentId("time_slot_view")
public class TimeSlotView extends View {
  private static final Logger log = LoggerFactory.getLogger(TimeSlotView.class);

  @Query("""
      SELECT * as timeSlots
        FROM time_slot_view
        LIMIT 1000
          """)
  public QueryEffect<TimeSlots> getAllTimeSlots() {
    log.info("Getting all time slots");
    return queryResult();
  }

  @Query("""
      SELECT * as timeSlots
        FROM time_slot_view
        WHERE participantType = :participantType
          AND startTime >= :timeBegin
          AND startTime < :timeEnd
          """)
  public QueryEffect<TimeSlots> getTimeSlotsByParticipantTypeAndTimeRange(ByParticipantTypeAndTimeRange byTypeAndTimeRange) {
    log.info("{}", byTypeAndTimeRange);
    return queryResult();
  }

  @Query("""
      SELECT * as timeSlots
        FROM time_slot_view
        WHERE participantId = :participantId
          AND participantType = :participantType
          AND startTime >= :timeBegin
          AND startTime < :timeEnd
          """)
  public QueryEffect<TimeSlots> getTimeSlotsByParticipantAndTimeRange(ByParticipantAndTimeRange byParticipantAndTimeRange) {
    return queryResult();
  }

  @Consume.FromEventSourcedEntity(TimeSlotEntity.class)
  public static class TimeSlotsByDate extends TableUpdater<TimeSlotRow> {

    public Effect<TimeSlotRow> onEvent(TimeSlot.Event event) {
      return switch (event) {
        case TimeSlot.Event.TimeSlotMadeAvailable e -> effects().updateRow(onEvent(e));
        case TimeSlot.Event.TimeSlotMadeUnavailable e -> effects().updateRow(onEvent(e));
        case TimeSlot.Event.TimeSlotReservationCancelled e -> effects().updateRow(onEvent(e));
        case TimeSlot.Event.StudentRequestAccepted e -> effects().updateRow(onEvent(e));
        case TimeSlot.Event.StudentRequestRejected e -> effects().ignore();
        case TimeSlot.Event.InstructorRequestAccepted e -> effects().updateRow(onEvent(e));
        case TimeSlot.Event.InstructorRequestRejected e -> effects().ignore();
        case TimeSlot.Event.AircraftRequestAccepted e -> effects().updateRow(onEvent(e));
        case TimeSlot.Event.AircraftRequestRejected e -> effects().ignore();
        default -> effects().ignore();
      };
    }

    private TimeSlotRow onEvent(TimeSlot.Event.TimeSlotMadeAvailable event) {
      log.info("Event: {}\n_State: {}", event, rowState());

      return new TimeSlotRow(
          event.timeSlotId(),
          event.startTime(),
          TimeSlot.Status.available.name(),
          event.participantId(),
          event.participantType().name(),
          null);
    }

    private TimeSlotRow onEvent(TimeSlot.Event.TimeSlotMadeUnavailable event) {
      log.info("Event: {}\n_State: {}", event, rowState());

      return new TimeSlotRow(
          event.timeSlotId(),
          Instant.EPOCH,
          TimeSlot.Status.unavailable.name(),
          null,
          null,
          null);
    }

    private TimeSlotRow onEvent(TimeSlot.Event.TimeSlotReservationCancelled event) {
      log.info("Event: {}\n_State: {}", event, rowState());

      return new TimeSlotRow(
          rowState().timeSlotId(),
          rowState().startTime(),
          TimeSlot.Status.available.name(),
          rowState().participantId(),
          rowState().participantType(),
          null);
    }

    private TimeSlotRow onEvent(TimeSlot.Event.StudentRequestAccepted event) {
      log.info("Event: {}\n_State: {}", event, rowState());

      return new TimeSlotRow(
          rowState().timeSlotId(),
          rowState().startTime(),
          TimeSlot.Status.scheduled.name(),
          rowState().participantId(),
          rowState().participantType(),
          event.reservationId());
    }

    private TimeSlotRow onEvent(TimeSlot.Event.InstructorRequestAccepted event) {
      log.info("Event: {}\n_State: {}", event, rowState());

      return new TimeSlotRow(
          rowState().timeSlotId(),
          rowState().startTime(),
          TimeSlot.Status.scheduled.name(),
          rowState().participantId(),
          rowState().participantType(),
          event.reservationId());
    }

    private TimeSlotRow onEvent(TimeSlot.Event.AircraftRequestAccepted event) {
      log.info("Event: {}\n_State: {}", event, rowState());

      return new TimeSlotRow(
          rowState().timeSlotId(),
          rowState().startTime(),
          TimeSlot.Status.scheduled.name(),
          rowState().participantId(),
          rowState().participantType(),
          event.reservationId());
    }
  }

  public record TimeSlots(List<TimeSlotRow> timeSlots) {}

  public record TimeSlotRow(
      String timeSlotId,
      Instant startTime,
      String status,
      String participantId,
      String participantType,
      String reservationId) {}

  public record ByParticipantTypeAndTimeRange(
      String participantType,
      Instant timeBegin,
      Instant timeEnd) {}

  public record ByParticipantAndTimeRange(
      String participantId,
      String participantType,
      Instant timeBegin,
      Instant timeEnd) {}
}
