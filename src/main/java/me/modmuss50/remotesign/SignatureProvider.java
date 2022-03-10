package me.modmuss50.remotesign;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public interface SignatureProvider {
    void sign(File file, OutputStream outputStream) throws IOException;
}
