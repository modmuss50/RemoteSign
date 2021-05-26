package me.modmuss50.remotesign;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface SignatureProvider {
    void sign(InputStream inputStream, OutputStream outputStream) throws IOException;
}
