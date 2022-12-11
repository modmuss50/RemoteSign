package me.modmuss50.remotesign;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class RemoteSignatureProvider implements SignatureProvider {
	private static final int MAX_RETRIES = 5;

	private final SignatureMethod signatureMethod;
	private final String serverUrl;
	private final String authKey;

	public RemoteSignatureProvider(SignatureMethod signatureMethod, String serverUrl, String authKey) {
		this.signatureMethod = Objects.requireNonNull(signatureMethod);
		this.serverUrl = Objects.requireNonNull(serverUrl);
		this.authKey = Objects.requireNonNull(authKey);
	}

	@Override
	public void sign(File file, OutputStream outputStream) throws IOException {
		IOException exception = null;

		for (int i = 0; i < MAX_RETRIES; i++) {
			byte[] bytes;

			try {
				bytes = signFile(file);
			} catch (IOException e) {
				if (exception == null) {
					exception = new IOException("Failed to sign: " + file);
				}

				exception.addSuppressed(e);

				// Try again
				continue;
			}

			// Success
			IOUtils.copy(new ByteArrayInputStream(bytes), outputStream);
			return;
		}

		throw exception;
	}

	private byte[] signFile(File file) throws IOException {
		final HttpClient httpClient = HttpClients.createDefault();
		final HttpPost request = new HttpPost(serverUrl);

		request.setEntity(MultipartEntityBuilder.create().addTextBody("op", signatureMethod.name()).addTextBody("key", authKey).addBinaryBody("file", file).build());

		HttpResponse response = httpClient.execute(request);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new IOException("Failed to post, return code: " + response.getStatusLine().getStatusCode());
		}

		// First make sure we can read all the content into memory
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		int copied = IOUtils.copy(response.getEntity().getContent(), outputStream);

		if (copied == 0) {
			throw new IOException("No data copied");
		}

		return outputStream.toByteArray();
	}
}
