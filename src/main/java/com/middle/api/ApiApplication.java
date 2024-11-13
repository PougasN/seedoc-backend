package com.middle.api;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.CareTeam;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.reflect.Method;


@SpringBootApplication
public class ApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
		FhirContext ctx = FhirContext.forR4(); // R4 version
		System.out.println("FHIR Version: " + ctx.getVersion().getVersion());
		System.out.println("HAPI FHIR Version: " + ctx.getVersion().getVersion());
	}
}

