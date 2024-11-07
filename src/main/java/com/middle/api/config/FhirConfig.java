package com.middle.api.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.middle.api.converter.FhirHttpMessageConverter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class FhirConfig implements WebMvcConfigurer {

    @Value("${hapi.fhir.server.url}")
    private String hapiFhirUrl;

    @Value("${react.frontend.url}")
    private String reactUrl;

    private final String[] ALLOWED_ORIGINS = {
        reactUrl,
        "http://localhost:3000"
    };

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    public IGenericClient fhirClient(FhirContext fhirContext) {
        return fhirContext.newRestfulGenericClient(hapiFhirUrl);
    }

    @Bean
    public HttpMessageConverter<IBaseResource> fhirHttpMessageConverter(FhirContext fhirContext) {
        return new FhirHttpMessageConverter(fhirContext);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(ALLOWED_ORIGINS)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
            }
        };
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(fhirHttpMessageConverter(fhirContext()));
    }
}
