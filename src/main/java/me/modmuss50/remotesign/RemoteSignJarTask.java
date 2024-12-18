package me.modmuss50.remotesign;

import org.gradle.api.DefaultTask;
import org.gradle.api.Transformer;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.concurrent.Callable;

public abstract class RemoteSignJarTask extends DefaultTask {
	@InputFile
	abstract RegularFileProperty getInput();

	@Input
	abstract Property<SignatureMethod> getSignatureMethod();

	@Input
	protected abstract Property<SignatureProvider> getSignatureProvider();

	@OutputFile
	abstract RegularFileProperty getOutput();

	@Inject
	protected abstract WorkerExecutor getWorkerExecutor();

	public RemoteSignJarTask() {
		getInput().finalizeValueOnRead();
		getOutput().finalizeValueOnRead();
		getSignatureMethod().finalizeValueOnRead();
		getSignatureProvider().set(getSignatureMethod().map(signatureMethod -> {
            final RemoteSignExtension extension = getProject().getExtensions().getByType(RemoteSignExtension.class);
            return extension.signatureProvider(signatureMethod);
        }));
	}

	@TaskAction
	public void doTask() {
		final WorkQueue workQueue = getWorkerExecutor().noIsolation();

		workQueue.submit(SignWorkAction.class, parameters -> {
			parameters.getInputFile().set(getInput());
			parameters.getOutputFile().set(getOutput());
			parameters.getSignatureProvider().set(getSignatureProvider().get());
		});
	}

	public interface SignWorkParameters extends WorkParameters {
		RegularFileProperty getInputFile();
		RegularFileProperty getOutputFile();

		Property<SignatureProvider> getSignatureProvider();
	}

	public abstract static class SignWorkAction implements WorkAction<SignWorkParameters> {
		@Override
		public void execute() {
			final File input = getParameters().getInputFile().getAsFile().get();
			final File output = getParameters().getOutputFile().get().getAsFile();

			if (!input.exists() || input.length() == 0) {
				throw new UncheckedIOException(new FileNotFoundException(input.getAbsolutePath() + " does not exist or is empty"));
			}

			try (OutputStream outputStream = Files.newOutputStream(output.toPath())) {
				getParameters().getSignatureProvider().get().sign(input, outputStream);
			} catch (IOException e) {
				throw new RuntimeException("Failed to sign jar", e);
			}
		}
	}
}
