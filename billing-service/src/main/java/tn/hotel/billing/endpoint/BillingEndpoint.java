package tn.hotel.billing.endpoint;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import tn.hotel.billing.config.SoapWebServiceConfig;
import tn.hotel.billing.entity.InvoiceEntity;
import tn.hotel.billing.exception.ReservationNotFoundException;
import tn.hotel.billing.model.*;
import tn.hotel.billing.repository.InvoiceRepository;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Endpoint
@RequiredArgsConstructor
public class BillingEndpoint {

    private final InvoiceRepository invoiceRepository;

    @PayloadRoot(namespace = SoapWebServiceConfig.NAMESPACE_URI, localPart = "generateInvoiceRequest")
    @ResponsePayload
    public GenerateInvoiceResponse generateInvoice(@RequestPayload GenerateInvoiceRequest request) {
        log.info("SOAP generateInvoiceRequest received for reservationId: {}, clientId: {}, nights: {}, pricePerNight: {}",
                request.getReservationId(), request.getClientId(), request.getNights(), request.getPricePerNight());

        // Check if an invoice already exists for this reservation
        InvoiceEntity invoiceEntity = invoiceRepository.findByReservationId(request.getReservationId())
                .orElseGet(() -> {
                    BigDecimal nights = new BigDecimal(request.getNights());
                    BigDecimal pricePerNight = request.getPricePerNight();
                    BigDecimal amountHT = nights.multiply(pricePerNight).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal tva = amountHT.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal amountTTC = amountHT.add(tva).setScale(2, RoundingMode.HALF_UP);

                    InvoiceEntity newEntity = InvoiceEntity.builder()
                            .invoiceId("inv-" + UUID.randomUUID())
                            .reservationId(request.getReservationId())
                            .clientId(request.getClientId())
                            .amountHT(amountHT)
                            .tva(tva)
                            .amountTTC(amountTTC)
                            .issueDate(LocalDate.now())
                            .status(InvoiceStatus.EMISE.value())
                            .build();

                    return invoiceRepository.save(newEntity);
                });

        Invoice invoiceDto = toInvoiceDto(invoiceEntity);
        GenerateInvoiceResponse response = new GenerateInvoiceResponse();
        response.setInvoice(invoiceDto);
        return response;
    }

    @PayloadRoot(namespace = SoapWebServiceConfig.NAMESPACE_URI, localPart = "getInvoiceRequest")
    @ResponsePayload
    public GetInvoiceResponse getInvoice(@RequestPayload GetInvoiceRequest request) {
        log.info("SOAP getInvoiceRequest received for reservationId: {}", request.getReservationId());

        InvoiceEntity invoiceEntity = invoiceRepository.findByReservationId(request.getReservationId())
                .orElseThrow(() -> new ReservationNotFoundException("No invoice found for reservation ID: " + request.getReservationId()));

        Invoice invoiceDto = toInvoiceDto(invoiceEntity);
        GetInvoiceResponse response = new GetInvoiceResponse();
        response.setInvoice(invoiceDto);
        return response;
    }

    private Invoice toInvoiceDto(InvoiceEntity entity) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(entity.getInvoiceId());
        invoice.setReservationId(entity.getReservationId());
        invoice.setAmountHT(entity.getAmountHT());
        invoice.setTva(entity.getTva());
        invoice.setAmountTTC(entity.getAmountTTC());
        invoice.setStatus(InvoiceStatus.fromValue(entity.getStatus()));

        try {
            LocalDate date = entity.getIssueDate();
            XMLGregorianCalendar xmlDate = DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                            0, 0, 0, 0, 0);
            invoice.setIssueDate(xmlDate);
        } catch (Exception e) {
            log.error("Failed to convert date to XMLGregorianCalendar", e);
        }

        return invoice;
    }
}
