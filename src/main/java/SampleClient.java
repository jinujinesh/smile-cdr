import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;

public class SampleClient {

    public static void main(String[] theArgs) {

        // Create a FHIR client
        FhirContext fhirContext = FhirContext.forR4();
        IGenericClient client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
        client.registerInterceptor(new LoggingInterceptor(false));

        // Search for Patient resources
        Bundle response = client
                .search()
                .forResource("Patient")
                .where(Patient.FAMILY.matches().value("SMITH"))
                .returnBundle(Bundle.class)
                .execute();
            
                List<BundleEntryComponent> bundleEntry  = response.getEntry();
                List<Patient> patientsList = new ArrayList<Patient>();

                bundleEntry.forEach(be -> patientsList.add((Patient)be.getResource()));


                Comparator<Patient> patientNameComparator = ((p1, p2) -> p1.getName().get(0).getGiven().get(0).getValueAsString().compareTo(p2.getName().get(0).getGiven().get(0).getValueAsString()));
                patientsList.sort(patientNameComparator);

                // Basic tasks - listing of patients' first, last and dob & ordering by first name
                patientsList.forEach(patient ->{
                    System.out.println(patient.getName().get(0).getGiven().get(0).getValueAsString()
                    + " | "+ patient.getName().get(0).getFamily()
                    + " | "+ patient.getBirthDate());
                });



    }

}
