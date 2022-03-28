import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;

public class SampleClient implements IClientInterceptor{

    static Long totalTime = 0L;
    static int loop = 0;
    static Map<Integer, Long> avgLoopResponseTime = null; 
    public static void main(String[] theArgs) {
        // Search for Patient resources
        Stream<String> nameContents = null;
        avgLoopResponseTime = new HashMap<Integer, Long>();

        for(loop = 1; loop < 4; loop++){
            System.out.println("Loop #" + loop);
            try {
                nameContents = Files.lines(Paths.get("lastnames.txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            nameContents.forEach(a -> patientTasks(a));

            System.out.println("Total execution time: "+ totalTime);

            System.out.println("The average response time for 20 searches: "+ totalTime/20);
            avgLoopResponseTime.put(Integer.valueOf(loop), totalTime/20);
        }

        avgLoopResponseTime.forEach((k, v) ->{
            System.out.println("Loop #:" +k +" Average time :"+ v);
        });

        //Execute Unit test
        assertProcessResponses(avgLoopResponseTime);
    }
    public static void patientTasks(String familyName) {
        // Create a FHIR client
        FhirContext fhirContext = FhirContext.forR4();
        IGenericClient client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
        client.registerInterceptor(new LoggingInterceptor(false));
        client.registerInterceptor(new SampleClient());

        Bundle response = client
                .search()
                .forResource("Patient")
                .where(Patient.FAMILY.matches().value(familyName))
                .returnBundle(Bundle.class)
                .execute();

        List<BundleEntryComponent> bundleEntry = response.getEntry();
        List<Patient> patientsList = new ArrayList<Patient>();

        bundleEntry.forEach(be -> {
            Patient patient = (Patient) be.getResource();
            if (patient.getName().get(0) != null) {
                if (patient.getName().get(0).getGiven().size() > 0) {
                    if (null != patient.getName().get(0).getGiven().get(0))
                        patientsList.add(patient);
                }
            }
        });

        Comparator<Patient> patientNameComparator = null;
        if (patientsList != null && patientsList.size() > 1) {
            patientNameComparator = ((p1, p2) -> p1.getName().get(0).getGiven().get(0).getValueAsString()
                    .compareTo(p2.getName().get(0).getGiven().get(0).getValueAsString()));
        }

        patientsList.sort(patientNameComparator);
        // Basic tasks - listing of patients' first, last and dob & ordering by first
        // name
        if (patientsList != null && patientsList.size() > 0)
            patientsList.forEach(patient -> {
                System.out.println(patient.getName().get(0).getGiven().get(0).getValueAsString()
                        + " | " + patient.getName().get(0).getFamily()
                        + " | " + patient.getBirthDate());
            });
    }

    @Override
    public void interceptRequest(IHttpRequest theRequest) {
        if(loop == 3)
            theRequest.addHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        else
            theRequest.addHeader("Cache-Control","max-age=60, must-revalidate, no-transform");
     }

    @Override
    public void interceptResponse(IHttpResponse theResponse) throws IOException {
        totalTime += theResponse.getRequestStopWatch().getMillis();
    }

    public static void assertProcessResponses(Map<Integer, Long> avgResponseTimes){
        if(avgResponseTimes.get(3) > avgResponseTimes.get(1) 
           && avgResponseTimes.get(3) > avgResponseTimes.get(2))
           System.out.println("Expected == Actual : Test PASSED");
        else
            System.out.println("Expected != Actual : Test FAILED");
    }

}
