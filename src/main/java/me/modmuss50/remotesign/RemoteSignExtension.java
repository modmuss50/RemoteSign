package me.modmuss50.remotesign;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.gradle.api.Project;
import org.gradle.api.publish.Publication;
import org.gradle.api.publish.PublicationArtifact;
import org.gradle.api.publish.internal.PublicationInternal;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public abstract class RemoteSignExtension {
	private final Project project;
	String requestUrl;
	String pgpAuthKey;
	String jarAuthKey;

	public RemoteSignExtension(Project project) {
		this.project = project;
	}

	public void sign(Publication... publications) {
		for (Publication publication : publications) {
			signArtifact((PublicationInternal<?>) publication);
		}
	}

	public void sign(AbstractArchiveTask... tasks) {
		for (AbstractArchiveTask task : tasks) {
			project.getTasks().create("sign" + StringUtils.capitalize(task.getName()), RemoteSignJarTask.class, remoteSignJarTask -> {
				remoteSignJarTask.setInput(task.getArchiveFile());
				remoteSignJarTask.getArchiveClassifier().set(task.getArchiveClassifier().map(s -> s + "Signed").getOrElse("signed"));
				remoteSignJarTask.setGroup("sign");
				remoteSignJarTask.dependsOn(task);
			});
		}
	}

	private <T extends PublicationArtifact> void signArtifact(PublicationInternal<T> publication) {
		for (T artifact : publication.getPublishableArtifacts()) {
			publication.addDerivedArtifact(artifact, new SignedArtifact(artifact));
		}
	}

	void requestUrl(String requestUrl) {
		this.requestUrl = requestUrl;
	}

	void pgpAuthKey(String pgpAuthKey) {
		this.pgpAuthKey = pgpAuthKey;
	}

	void jarAuthKey(String jarAuthKey) {
		this.jarAuthKey = jarAuthKey;
	}

	private class SignedArtifact implements PublicationInternal.DerivedArtifact {
		private final PublicationArtifact artifact;
		private boolean generated = false;

		SignedArtifact(PublicationArtifact artifact) {
			this.artifact = artifact;
		}

		@Override
		public boolean shouldBePublished() {
			return true;
		}

		@Override
		public File create() {
			File file = new File(artifact.getFile().getAbsolutePath() + ".asc");

			if (generated) {
				return file;
			}

			generated = true;
			Objects.requireNonNull(requestUrl, "No requestUrl provided");
			Objects.requireNonNull(pgpAuthKey, "No authKey provided");

			HttpPost request = new HttpPost(requestUrl);
			request.setEntity(
					MultipartEntityBuilder.create()
							.addTextBody("op", "PGPSIGN")
							.addTextBody("key", pgpAuthKey)
							.addBinaryBody("file", artifact.getFile())
							.build()
			);

			final HttpClient client = HttpClients.createDefault();

			try {
				HttpResponse response = client.execute(request);
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new RuntimeException("Failed to get, return code: " + response.getStatusLine().getStatusCode());
				}

				try (OutputStream outputStream = new FileOutputStream(file)) {
					IOUtils.copy(response.getEntity().getContent(), outputStream);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			return file;
		}
	}
}
