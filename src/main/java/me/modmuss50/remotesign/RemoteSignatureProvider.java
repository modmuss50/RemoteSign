package me.modmuss50.remotesign;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.File;
import java.io.IOException;
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
    public void sign(File file, OutputStream outputStream) throws IOException {
        final HttpPost request = new HttpPost(serverUrl);

        request.setEntity(
                MultipartEntityBuilder.create()
                        .addTextBody("op", signatureMethod.name())
                        .addTextBody("key", authKey)
                        .addBinaryBody("file", file)
                        .build()
        );

        HttpResponse response = httpClient.execute(request);

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IOException("Failed to post, return code: " + response.getStatusLine().getStatusCode());
        }

        int copied = IOUtils.copy(response.getEntity().getContent(), outputStream);

        if (copied == 0) {
            throw new IOException("No data copied");
        }
    }
}
