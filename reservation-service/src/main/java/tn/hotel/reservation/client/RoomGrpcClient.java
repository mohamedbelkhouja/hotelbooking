package tn.hotel.reservation.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tn.hotel.reservation.dto.RoomDto;
import tn.hotel.reservation.dto.RoomType;
import tn.hotel.reservation.exception.ServiceUnavailableException;
import tn.hotel.room.v1.*;

import jakarta.annotation.PreDestroy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RoomGrpcClient {

    private final String host;
    private final int port;
    private final long deadlineMs;
    private final int maxAttempts;
    private final long initialBackoffMs;
    private final double backoffMultiplier;

    private ManagedChannel channel;
    private RoomServiceGrpc.RoomServiceBlockingStub blockingStub;

    public RoomGrpcClient(
            @Value("${room.grpc.host:room-service}") String host,
            @Value("${room.grpc.port:9090}") int port,
            @Value("${room.grpc.deadline-ms:2000}") long deadlineMs,
            @Value("${room.grpc.retry.max-attempts:3}") int maxAttempts,
            @Value("${room.grpc.retry.initial-backoff-ms:200}") long initialBackoffMs,
            @Value("${room.grpc.retry.multiplier:2.0}") double backoffMultiplier) {
        this.host = host;
        this.port = port;
        this.deadlineMs = deadlineMs;
        this.maxAttempts = maxAttempts;
        this.initialBackoffMs = initialBackoffMs;
        this.backoffMultiplier = backoffMultiplier;

        initChannel();
    }

    private synchronized void initChannel() {
        if (channel == null || channel.isShutdown() || channel.isTerminated()) {
            channel = ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .build();
            blockingStub = RoomServiceGrpc.newBlockingStub(channel);
        }
    }

    public CheckAvailabilityResponse checkAvailability(String roomId, LocalDate checkIn, LocalDate checkOut) {
        CheckAvailabilityRequest request = CheckAvailabilityRequest.newBuilder()
                .setRoomId(roomId)
                .setCheckIn(checkIn.toString())
                .setCheckOut(checkOut.toString())
                .build();

        long currentBackoff = initialBackoffMs;
        StatusRuntimeException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                initChannel();
                return blockingStub
                        .withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                        .checkAvailability(request);
            } catch (StatusRuntimeException e) {
                lastException = e;
                log.warn("CheckAvailability attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(currentBackoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    currentBackoff = (long) (currentBackoff * backoffMultiplier);
                }
            }
        }

        log.error("Failed to connect to room-service after {} attempts: {}", maxAttempts, lastException != null ? lastException.getMessage() : "Unknown");
        throw new ServiceUnavailableException("room-service is unavailable (timeout or connection error): " +
                (lastException != null ? lastException.getMessage() : ""));
    }

    public List<RoomDto> streamAvailableRooms(LocalDate checkIn, LocalDate checkOut, RoomType type, Integer minCapacity) {
        AvailableRoomsRequest.Builder builder = AvailableRoomsRequest.newBuilder()
                .setCheckIn(checkIn.toString())
                .setCheckOut(checkOut.toString());

        if (type != null) {
            builder.setType(type.name());
        }
        if (minCapacity != null) {
            builder.setMinCapacity(minCapacity);
        }

        List<RoomDto> result = new ArrayList<>();
        try {
            initChannel();
            Iterator<Room> roomIterator = blockingStub
                    .withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                    .streamAvailableRooms(builder.build());

            while (roomIterator.hasNext()) {
                Room r = roomIterator.next();
                result.add(RoomDto.builder()
                        .id(r.getId())
                        .number(r.getNumber())
                        .type(RoomType.valueOf(r.getType()))
                        .capacity(r.getCapacity())
                        .pricePerNight(r.getPricePerNight())
                        .build());
            }
            return result;
        } catch (StatusRuntimeException e) {
            log.error("Error streaming available rooms from room-service: {}", e.getMessage());
            throw new ServiceUnavailableException("room-service is unavailable: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error streaming rooms", e);
            throw new ServiceUnavailableException("Failed to stream available rooms: " + e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}
