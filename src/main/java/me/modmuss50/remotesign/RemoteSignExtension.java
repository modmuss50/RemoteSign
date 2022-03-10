package me.modmuss50.remotesign;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.publish.Publication;
import org.gradle.api.publish.PublicationArtifact;
import org.gradle.api.publish.internal.PublicationInternal;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

import java.io.*;

public abstract class RemoteSignExtension {
	private final Project project;

	abstract Property<String> getRequestUrl();
	abstract Property<String> getPgpAuthKey();
	abstract Property<String> getJarAuthKey();

	public RemoteSignExtension(Project project) {
		this.project = project;

		getRequestUrl().finalizeValueOnRead();
		getPgpAuthKey().finalizeValueOnRead();
		getJarAuthKey().finalizeValueOnRead();
	}

	public void sign(Publication... publications) {
		for (Publication publication : publications) {
			signArtifact((PublicationInternal<?>) publication);
		}
	}

	public void sign(AbstractArchiveTask... tasks) {
		for (AbstractArchiveTask task : tasks) {
			project.getTasks().register("sign" + StringUtils.capitalize(task.getName()), RemoteSignJarTask.class, remoteSignJarTask -> {
				remoteSignJarTask.getInput().set(task.getArchiveFile());
				remoteSignJarTask.getOutput().set(task.getArchiveFile()); // Replace the old file
				remoteSignJarTask.getSignatureMethod().set(SignatureMethod.JARSIGN);
				remoteSignJarTask.setGroup("sign");
				remoteSignJarTask.dependsOn(task);
			});
		}
	}

	public TaskProvider<RemoteSignJarTask> sign(File inputFile, File outputFile, String name) {
		return project.getTasks().register("sign" + StringUtils.capitalize(name), RemoteSignJarTask.class, remoteSignJarTask -> {
			remoteSignJarTask.getInput().set(inputFile);
			remoteSignJarTask.getOutput().set(outputFile);
			remoteSignJarTask.getSignatureMethod().set(SignatureMethod.JARSIGN);
			remoteSignJarTask.setGroup("sign");
		});
	}

	private <T extends PublicationArtifact> void signArtifact(PublicationInternal<T> publication) {
		String taskNamePrefix = "sign" + StringGroovyMethods.capitalize(publication.getName());

		int i = 0;
		for (T artifact : publication.getPublishableArtifacts()) {
			TaskProvider<RemoteSignJarTask> task = project.getTasks().register(taskNamePrefix + i++, RemoteSignJarTask.class, remoteSignJarTask -> {
				remoteSignJarTask.getInput().set(artifact.getFile());
				remoteSignJarTask.getOutput().set(new File(artifact.getFile().getAbsolutePath() + ".asc"));
				remoteSignJarTask.getSignatureMethod().set(SignatureMethod.PGPSIGN);
				remoteSignJarTask.setGroup("sign");
			});

			T derivedArtifact = publication.addDerivedArtifact(artifact, new SignedArtifact(task));
			derivedArtifact.builtBy(task);
		}
	}

	@Internal
	public SignatureProvider signatureProvider(SignatureMethod method) {
		return new RemoteSignatureProvider(
				HttpClients.createDefault(),
				method, getRequestUrl().get(),
				method == SignatureMethod.PGPSIGN ? getPgpAuthKey().get() : getJarAuthKey().get()
		);
	}

	private class SignedArtifact implements PublicationInternal.DerivedArtifact {
		private final TaskProvider<RemoteSignJarTask> task;

		private SignedArtifact(TaskProvider<RemoteSignJarTask> task) {
			this.task = task;
		}

		@Override
		public boolean shouldBePublished() {
			return task.get().isEnabled();
		}

		@Override
		public File create() {
			return task.get().getOutput().get().getAsFile();
		}
	}
}
