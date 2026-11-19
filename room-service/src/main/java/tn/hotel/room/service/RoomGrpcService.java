package tn.hotel.room.service;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import tn.hotel.room.entity.RoomBookingEntity;
import tn.hotel.room.entity.RoomEntity;
import tn.hotel.room.repository.RoomBookingRepository;
import tn.hotel.room.repository.RoomRepository;
import tn.hotel.room.v1.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RoomGrpcService extends RoomServiceGrpc.RoomServiceImplBase {

    private final RoomRepository roomRepository;
    private final RoomBookingRepository roomBookingRepository;

    @Override
    public void checkAvailability(CheckAvailabilityRequest request,
                                  StreamObserver<CheckAvailabilityResponse> responseObserver) {
        log.info("Received checkAvailability request for room_id: {}, period: {} to {}",
                request.getRoomId(), request.getCheckIn(), request.getCheckOut());

        try {
            LocalDate checkIn = LocalDate.parse(request.getCheckIn());
            LocalDate checkOut = LocalDate.parse(request.getCheckOut());

            if (!checkOut.isAfter(checkIn)) {
                CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                        .setAvailable(false)
                        .setPricePerNight(0.0)
                        .setReason("checkOut date must be after checkIn date")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            Optional<RoomEntity> optionalRoom = roomRepository.findById(request.getRoomId());
            if (optionalRoom.isEmpty()) {
                CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                        .setAvailable(false)
                        .setPricePerNight(0.0)
                        .setReason("Room not found with id: " + request.getRoomId())
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            RoomEntity room = optionalRoom.get();
            List<RoomBookingEntity> overlapping = roomBookingRepository.findOverlappingBookings(
                    room.getId(), checkIn, checkOut);

            if (!overlapping.isEmpty()) {
                CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                        .setAvailable(false)
                        .setPricePerNight(room.getPricePerNight().doubleValue())
                        .setReason("Room is already booked for the selected period")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                    .setAvailable(true)
                    .setPricePerNight(room.getPricePerNight().doubleValue())
                    .setReason("")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (DateTimeParseException e) {
            log.error("Date format error in checkAvailability: {}", e.getMessage());
            CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                    .setAvailable(false)
                    .setPricePerNight(0.0)
                    .setReason("Invalid date format. Expected yyyy-MM-dd")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing checkAvailability", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void streamAvailableRooms(AvailableRoomsRequest request,
                                     StreamObserver<Room> responseObserver) {
        log.info("Received streamAvailableRooms request for period: {} to {}, type: {}, min_capacity: {}",
                request.getCheckIn(), request.getCheckOut(), request.getType(), request.getMinCapacity());

        try {
            LocalDate checkIn = LocalDate.parse(request.getCheckIn());
            LocalDate checkOut = LocalDate.parse(request.getCheckOut());

            String type = request.getType().isBlank() ? null : request.getType();
            Integer minCapacity = request.getMinCapacity() > 0 ? request.getMinCapacity() : null;

            List<RoomEntity> availableRooms = roomRepository.findAvailableRooms(
                    checkIn, checkOut, type, minCapacity);

            for (RoomEntity r : availableRooms) {
                Room roomProto = Room.newBuilder()
                        .setId(r.getId())
                        .setNumber(r.getNumber())
                        .setType(r.getType())
                        .setCapacity(r.getCapacity())
                        .setPricePerNight(r.getPricePerNight().doubleValue())
                        .build();
                responseObserver.onNext(roomProto);
            }

            responseObserver.onCompleted();
            log.info("Streamed {} available rooms successfully", availableRooms.size());

        } catch (DateTimeParseException e) {
            log.error("Date format error in streamAvailableRooms: {}", e.getMessage());
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription("Invalid date format. Expected yyyy-MM-dd")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error streaming available rooms", e);
            responseObserver.onError(e);
        }
    }
}
