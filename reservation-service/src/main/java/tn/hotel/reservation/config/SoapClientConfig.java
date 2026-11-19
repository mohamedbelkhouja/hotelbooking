package tn.hotel.reservation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

@Configuration
public class SoapClientConfig {

    @Value("${billing.soap-uri:http://billing-service:8083/ws}")
    private String billingSoapUri;

    @Bean
    public Jaxb2Marshaller billingMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("tn.hotel.reservation.soap.model");
        return marshaller;
    }

    @Bean
    public WebServiceTemplate billingWebServiceTemplate(Jaxb2Marshaller billingMarshaller) {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(billingMarshaller);
        template.setUnmarshaller(billingMarshaller);
        template.setDefaultUri(billingSoapUri);
        return template;
    }
}
