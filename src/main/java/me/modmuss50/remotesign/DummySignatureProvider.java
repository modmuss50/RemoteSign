package me.modmuss50.remotesign;

import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class DummySignatureProvider implements SignatureProvider {
    private final Project project;
    private final SignatureMethod signatureMethod;

    public DummySignatureProvider(Project project, SignatureMethod signatureMethod) {
        this.project = project;
        this.signatureMethod = signatureMethod;
    }

    @Override
    public void sign(File file, OutputStream outputStream) throws IOException {
        project.getLogger().lifecycle("Dummy signing ({}) with {}", file.getName(), signatureMethod);

        Files.copy(file.toPath(), outputStream);
    }
}
