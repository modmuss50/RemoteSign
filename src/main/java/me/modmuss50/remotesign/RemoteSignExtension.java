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

import java.io.File;

public abstract class RemoteSignExtension {
	private final Project project;

	abstract Property<String> getRequestUrl();
	abstract Property<String> getPgpAuthKey();
	abstract Property<String> getJarAuthKey();

	// Enable to test locally without actually signing.
	abstract Property<Boolean> getUseDummyForTesting();

	private final File tempDir;

	public RemoteSignExtension(Project project) {
		this.project = project;

		getRequestUrl().finalizeValueOnRead();
		getPgpAuthKey().finalizeValueOnRead();
		getJarAuthKey().finalizeValueOnRead();

		getUseDummyForTesting().convention(false).finalizeValueOnRead();

		tempDir = new File(project.getBuildDir(), "remotesign");
	}

	public void sign(Publication... publications) {
		for (Publication publication : publications) {
			signArtifact((PublicationInternal<?>) publication);
		}
	}

	public void sign(AbstractArchiveTask... tasks) {
		for (AbstractArchiveTask task : tasks) {
			String name = "sign" + StringUtils.capitalize(task.getName());
			project.getTasks().register(name, RemoteSignJarTask.class, remoteSignJarTask -> {
				remoteSignJarTask.getInput().set(task.getArchiveFile());
				remoteSignJarTask.getOutput().set(getOutputFile(name, task.getArchiveFile().get().getAsFile()));
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
			final String name = taskNamePrefix + i++;

			TaskProvider<RemoteSignJarTask> task = project.getTasks().register(name, RemoteSignJarTask.class, remoteSignJarTask -> {
				remoteSignJarTask.getInput().set(artifact.getFile());
				remoteSignJarTask.getOutput().set(getOutputFile(name, artifact.getFile(), "asc"));
				remoteSignJarTask.getSignatureMethod().set(SignatureMethod.PGPSIGN);
				remoteSignJarTask.setGroup("sign");

				remoteSignJarTask.dependsOn(artifact.getBuildDependencies());
			});

			T derivedArtifact = publication.addDerivedArtifact(artifact, new SignedArtifact(task));
			derivedArtifact.builtBy(task);
		}
	}

	private File getOutputFile(String name, File input) {
		return new File(tempDir, name + "/" + input.getName());
	}

	private File getOutputFile(String name, File input, String ext) {
		return new File(tempDir, name + "/" + input.getName() + "." + ext);
	}

	@Internal
	public SignatureProvider signatureProvider(SignatureMethod method) {
		if (getUseDummyForTesting().get()) {
			return new DummySignatureProvider(method);
		}

		return new RemoteSignatureProvider(
				method, getRequestUrl().get(),
				method == SignatureMethod.PGPSIGN ? getPgpAuthKey().get() : getJarAuthKey().get()
		);
	}

	private static class SignedArtifact implements PublicationInternal.DerivedArtifact {
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
