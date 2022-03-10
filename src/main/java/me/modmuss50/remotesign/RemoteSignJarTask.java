package me.modmuss50.remotesign;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.*;

public abstract class RemoteSignJarTask extends DefaultTask {
	@InputFile
	abstract RegularFileProperty getInput();

	@Input
	abstract Property<SignatureMethod> getSignatureMethod();

	@OutputFile
	abstract RegularFileProperty getOutput();

	public RemoteSignJarTask() {
		getInput().finalizeValueOnRead();
		getSignatureMethod().finalizeValueOnRead();
	}

	@TaskAction
	public void doTask() {
		RemoteSignExtension extension = getProject().getExtensions().getByType(RemoteSignExtension.class);

		getInput().finalizeValue();
		File output = this.getOutput().get().getAsFile();

		SignatureProvider signatureProvider = extension.signatureProvider(getSignatureMethod().get());

		try (OutputStream outputStream = new FileOutputStream(output)) {
			signatureProvider.sign(getInput().getAsFile().get(), outputStream);
		} catch (IOException e) {
			throw new RuntimeException("Failed to sign jar", e);
		}
	}
}
