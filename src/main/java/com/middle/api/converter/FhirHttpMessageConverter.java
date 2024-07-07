package com.middle.api.converter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.jetbrains.annotations.NotNull;
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
    protected boolean supports(@NotNull Class<?> clazz) {
        return IBaseResource.class.isAssignableFrom(clazz);
    }

    @NotNull
    @Override
    protected IBaseResource readInternal(@NotNull Class<? extends IBaseResource> clazz, @NotNull HttpInputMessage inputMessage) throws IOException {
        IParser parser = fhirContext.newJsonParser();
        try (InputStreamReader reader = new InputStreamReader(inputMessage.getBody(), StandardCharsets.UTF_8)) {
            return parser.parseResource(clazz, reader);
        }
    }

    @Override
    protected void writeInternal(@NotNull IBaseResource resource, HttpOutputMessage outputMessage) throws IOException {
        IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
        try (OutputStreamWriter writer = new OutputStreamWriter(outputMessage.getBody(), StandardCharsets.UTF_8)) {
            parser.encodeResourceToWriter(resource, writer);
        }
    }
}
