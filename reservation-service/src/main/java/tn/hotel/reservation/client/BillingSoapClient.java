package tn.hotel.reservation.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;
import tn.hotel.reservation.dto.InvoiceDto;
import tn.hotel.reservation.dto.InvoiceStatus;
import tn.hotel.reservation.exception.ResourceNotFoundException;
import tn.hotel.reservation.soap.model.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingSoapClient {

    private final WebServiceTemplate billingWebServiceTemplate;

    @Value("${billing.soap-uri:http://billing-service:8083/ws}")
    private String billingSoapUri;

    @CircuitBreaker(name = "billing", fallbackMethod = "generateInvoiceFallback")
    public InvoiceDto generateInvoice(String reservationId, String clientId, int nights, BigDecimal pricePerNight) {
        log.info("Calling SOAP billing-service generateInvoice for reservation: {}", reservationId);

        GenerateInvoiceRequest request = new GenerateInvoiceRequest();
        request.setReservationId(reservationId);
        request.setClientId(clientId);
        request.setNights(BigInteger.valueOf(nights));
        request.setPricePerNight(pricePerNight);

        GenerateInvoiceResponse response = (GenerateInvoiceResponse) billingWebServiceTemplate
                .marshalSendAndReceive(billingSoapUri, request);

        if (response != null && response.getInvoice() != null) {
            return toInvoiceDto(response.getInvoice());
        }

        throw new RuntimeException("Empty response received from SOAP billing service");
    }

    public InvoiceDto generateInvoiceFallback(String reservationId, String clientId, int nights, BigDecimal pricePerNight, Throwable t) {
        log.warn("Resilience4j fallback triggered for generateInvoice [reservationId={}]. Cause: {}", reservationId, t.getMessage());
        return InvoiceDto.builder()
                .invoiceId("PENDING-" + reservationId)
                .reservationId(UUID.fromString(reservationId))
                .amountHT(pricePerNight.multiply(BigDecimal.valueOf(nights)).doubleValue())
                .tva(pricePerNight.multiply(BigDecimal.valueOf(nights)).multiply(BigDecimal.valueOf(0.20)).doubleValue())
                .amountTTC(pricePerNight.multiply(BigDecimal.valueOf(nights)).multiply(BigDecimal.valueOf(1.20)).doubleValue())
                .issueDate(LocalDate.now())
                .status(InvoiceStatus.EN_ATTENTE)
                .build();
    }

    public InvoiceDto getInvoice(String reservationId) {
        log.info("Calling SOAP billing-service getInvoice for reservation: {}", reservationId);

        GetInvoiceRequest request = new GetInvoiceRequest();
        request.setReservationId(reservationId);

        try {
            GetInvoiceResponse response = (GetInvoiceResponse) billingWebServiceTemplate
                    .marshalSendAndReceive(billingSoapUri, request);

            if (response != null && response.getInvoice() != null) {
                return toInvoiceDto(response.getInvoice());
            }
            throw new ResourceNotFoundException("Invoice not found for reservation ID: " + reservationId);
        } catch (SoapFaultClientException e) {
            log.warn("SOAP Fault received: {}", e.getMessage());
            throw new ResourceNotFoundException("Invoice not found for reservation ID: " + reservationId + " (SOAP Fault)");
        } catch (Exception e) {
            log.error("Error communicating with SOAP billing-service: {}", e.getMessage());
            throw new ResourceNotFoundException("Could not retrieve invoice: " + e.getMessage());
        }
    }

    private InvoiceDto toInvoiceDto(Invoice soapInvoice) {
        LocalDate issueDate = null;
        XMLGregorianCalendar xmlDate = soapInvoice.getIssueDate();
        if (xmlDate != null) {
            issueDate = LocalDate.of(xmlDate.getYear(), xmlDate.getMonth(), xmlDate.getDay());
        }

        return InvoiceDto.builder()
                .invoiceId(soapInvoice.getInvoiceId())
                .reservationId(UUID.fromString(soapInvoice.getReservationId()))
                .amountHT(soapInvoice.getAmountHT() != null ? soapInvoice.getAmountHT().doubleValue() : null)
                .tva(soapInvoice.getTva() != null ? soapInvoice.getTva().doubleValue() : null)
                .amountTTC(soapInvoice.getAmountTTC() != null ? soapInvoice.getAmountTTC().doubleValue() : null)
                .issueDate(issueDate)
                .status(soapInvoice.getStatus() != null ? InvoiceStatus.valueOf(soapInvoice.getStatus().value()) : InvoiceStatus.EN_ATTENTE)
                .build();
    }
}
