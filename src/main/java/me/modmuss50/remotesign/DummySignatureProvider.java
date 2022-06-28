package me.modmuss50.remotesign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class DummySignatureProvider implements SignatureProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DummySignatureProvider.class);

    private final SignatureMethod signatureMethod;

    public DummySignatureProvider(SignatureMethod signatureMethod) {
        this.signatureMethod = signatureMethod;
    }

    @Override
    public void sign(File file, OutputStream outputStream) throws IOException {
	    LOGGER.info("Dummy signing ({}) with {}", file.getName(), signatureMethod);

        Files.copy(file.toPath(), outputStream);
    }
}
