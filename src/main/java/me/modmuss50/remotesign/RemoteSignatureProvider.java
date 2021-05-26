package me.modmuss50.remotesign;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class RemoteSignatureProvider implements SignatureProvider {
    private final HttpClient httpClient;
    private final SignatureMethod signatureMethod;
    private final String serverUrl;
    private final String authKey;

    public RemoteSignatureProvider(HttpClient httpClient, SignatureMethod signatureMethod, String serverUrl, String authKey) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.signatureMethod = Objects.requireNonNull(signatureMethod);
        this.serverUrl = Objects.requireNonNull(serverUrl);
        this.authKey = Objects.requireNonNull(authKey);
    }

    @Override
    public void sign(InputStream inputStream, OutputStream outputStream) throws IOException {
        HttpPost request = new HttpPost(serverUrl);
        request.setEntity(
                MultipartEntityBuilder.create()
                        .addTextBody("op", signatureMethod.name())
                        .addTextBody("key", authKey)
                        .addBinaryBody("file", inputStream)
                        .build()
        );

        HttpResponse response = httpClient.execute(request);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IOException("Failed to get, return code: " + response.getStatusLine().getStatusCode());
        }

        // Need to close there here as we might be replacing that file with the output
        inputStream.close();

        int copied = IOUtils.copy(response.getEntity().getContent(), outputStream);

        if (copied == 0) {
            throw new IOException("No data copied");
        }
    }
}
