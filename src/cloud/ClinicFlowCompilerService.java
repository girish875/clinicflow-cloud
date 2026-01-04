package cloud;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import org.eclipse.xtext.generator.GeneratorContext;
import org.eclipse.xtext.generator.IFileSystemAccess2;
import org.eclipse.xtext.generator.InMemoryFileSystemAccess;

import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.validation.CheckMode;

import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.util.CancelIndicator;

import org.clinicflow.ClinicFlowStandaloneSetup;
import org.clinicflow.generator.ClinicFlowGenerator;

import com.google.inject.Injector;

public class ClinicFlowCompilerService {

    public byte[] compileToPng(String clinicflowText) throws Exception {

        // 1️ Setup Xtext + Injector
        Injector injector =
            new ClinicFlowStandaloneSetup()
                .createInjectorAndDoEMFRegistration();

        // 2️ Load DSL
        ResourceSet rs = new ResourceSetImpl();
        Resource resource =
            rs.createResource(URI.createURI("input.clinicflow"));

        resource.load(
            new ByteArrayInputStream(clinicflowText.getBytes()),
            null
        );

        // 3️ RUN SEMANTIC VALIDATION (CORRECT API)
        IResourceValidator validator =
            injector.getInstance(IResourceValidator.class);

        List<Issue> issues = validator.validate(
            resource,
            CheckMode.ALL,
            CancelIndicator.NullImpl
        );

        StringBuilder errors = new StringBuilder();
        for (Issue issue : issues) {
            if (issue.getSeverity() == Severity.ERROR) {
                errors.append(issue.getMessage()).append("\n");
            }
        }

        if (errors.length() > 0) {
            throw new RuntimeException(errors.toString());
        }

        // 4️ Run generator ONLY if valid
        ClinicFlowGenerator generator = new ClinicFlowGenerator();
        InMemoryFileSystemAccess fsa =
            new InMemoryFileSystemAccess();

        GeneratorContext context = new GeneratorContext();
        IFileSystemAccess2 fsa2 = (IFileSystemAccess2) fsa;

        generator.doGenerate(resource, fsa2, context);

        // 5️ Extract generated diagram
        Map<String, CharSequence> files = fsa.getTextFiles();
        String diagram =
            files.values().iterator().next().toString();

        File dotFile = File.createTempFile("workflow", ".dot");
        Files.writeString(dotFile.toPath(), diagram);

        // 6️ DOT → PNG
        File pngFile = File.createTempFile("workflow", ".png");

        ProcessBuilder pb = new ProcessBuilder(
            "dot",
            "-Tpng",
            dotFile.getAbsolutePath(),
            "-o",
            pngFile.getAbsolutePath()
        );

        pb.start().waitFor();

        return Files.readAllBytes(pngFile.toPath());
    }
}
