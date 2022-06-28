package me.modmuss50.remotesign;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

public interface SignatureProvider extends Serializable {
    void sign(File file, OutputStream outputStream) throws IOException;
}
