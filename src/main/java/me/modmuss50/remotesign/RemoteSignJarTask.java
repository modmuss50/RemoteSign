package me.modmuss50.remotesign;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.tasks.Jar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class RemoteSignJarTask extends Jar {
	private Object input;

	public RemoteSignJarTask() {
	}

	@TaskAction
	public void doTask() {
		RemoteSignExtension extension = getProject().getExtensions().getByType(RemoteSignExtension.class);

		Objects.requireNonNull(input, "No input provided");
		Objects.requireNonNull(extension.requestUrl, "No requestUrl provided");
		Objects.requireNonNull(extension.jarAuthKey, "No authKey provided");

		File output = this.getArchiveFile().get().getAsFile();

		HttpPost request = new HttpPost(extension.requestUrl);
		request.setEntity(
				MultipartEntityBuilder.create()
						.addTextBody("op", "JARSIGN")
						.addTextBody("key", extension.jarAuthKey)
						.addBinaryBody("file", getInput())
						.build()
		);

		final HttpClient client = HttpClients.createDefault();

		try {
			HttpResponse response = client.execute(request);
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("Failed to get, return code: " + response.getStatusLine().getStatusCode());
			}

			try (OutputStream outputStream = new FileOutputStream(output)) {
				IOUtils.copy(response.getEntity().getContent(), outputStream);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@InputFile
	public File getInput() {
		return getProject().file(input);
	}

	void setInput(Object input) {
		this.input = input;
	}
}
