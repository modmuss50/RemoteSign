package me.modmuss50.remotesign;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

class RemoteSignPlugin implements Plugin<Project> {
	public void apply(Project project) {
		project.getExtensions().create("remoteSign", RemoteSignExtension.class, project);
	}
}
