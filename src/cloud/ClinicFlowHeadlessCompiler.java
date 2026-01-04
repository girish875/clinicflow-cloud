package cloud;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import org.clinicflow.ClinicFlowStandaloneSetup;

public class ClinicFlowHeadlessCompiler {

    public static void main(String[] args) throws Exception {

        // 1. Sample ClinicFlow input (acts like cloud IDE input)
        String clinicflow = """
        workflow GPVisit
        type Outpatient {
            start -> CheckIn

            stage CheckIn:
                CHECK_IN by Admin

            stage Assessment:
                ASSESSMENT by Nurse

            stage Consultation:
                CONSULTATION by Doctor

            CheckIn -> Assessment
            Assessment -> Consultation

            end Completed
        }
        """;

        // 2. Initialise Xtext in HEADLESS mode
        ClinicFlowStandaloneSetup.doSetup();

        // 3. Load model from text
        ResourceSet resourceSet = new ResourceSetImpl();
        Resource resource = resourceSet.createResource(
                URI.createURI("input.clinicflow"));

        resource.load(
                new ByteArrayInputStream(clinicflow.getBytes()),
                null
        );

        System.out.println("✔ ClinicFlow model parsed successfully");

        // 4. Write DOT file (simulating generator output)
        File dotFile = new File("workflow.dot");
        try (FileWriter fw = new FileWriter(dotFile)) {
            fw.write("""
            digraph ClinicFlow {
              CheckIn -> Assessment;
              Assessment -> Consultation;
            }
            """);
        }

        System.out.println("✔ DOT file generated: " + dotFile.getAbsolutePath());

        // 5. Convert DOT → PNG using Graphviz
        File pngFile = new File("workflow.png");

        ProcessBuilder pb = new ProcessBuilder(
                "dot",
                "-Tpng",
                dotFile.getAbsolutePath(),
                "-o",
                pngFile.getAbsolutePath()
        );

        pb.inheritIO();
        int exit = pb.start().waitFor();

        if (exit == 0 && pngFile.exists()) {
            System.out.println("✔ PNG generated: " + pngFile.getAbsolutePath());
        } else {
            throw new RuntimeException("Graphviz PNG generation failed");
        }

        // 6. Optional: verify PNG exists
        System.out.println("PNG size = " + Files.size(pngFile.toPath()) + " bytes");
    }
}
