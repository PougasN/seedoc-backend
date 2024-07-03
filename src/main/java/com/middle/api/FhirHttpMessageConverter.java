package com.middle.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class FhirHttpMessageConverter extends AbstractHttpMessageConverter<IBaseResource> {

    private final FhirContext fhirContext;

    public FhirHttpMessageConverter(FhirContext fhirContext) {
        super(new MediaType("application", "fhir+json", StandardCharsets.UTF_8));
        this.fhirContext = fhirContext;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return IBaseResource.class.isAssignableFrom(clazz);
    }

    @Override
    protected IBaseResource readInternal(Class<? extends IBaseResource> clazz, HttpInputMessage inputMessage) throws IOException {
        IParser parser = fhirContext.newJsonParser();
        try (InputStreamReader reader = new InputStreamReader(inputMessage.getBody(), StandardCharsets.UTF_8)) {
            return parser.parseResource(clazz, reader);
        }
    }

    @Override
    protected void writeInternal(IBaseResource resource, HttpOutputMessage outputMessage) throws IOException {
        IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
        try (OutputStreamWriter writer = new OutputStreamWriter(outputMessage.getBody(), StandardCharsets.UTF_8)) {
            parser.encodeResourceToWriter(resource, writer);
        }
    }
}
