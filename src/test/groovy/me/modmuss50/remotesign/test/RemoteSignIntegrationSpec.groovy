package me.modmuss50.remotesign.test

import io.javalin.Javalin
import spock.lang.Shared
import spock.lang.Specification
import org.gradle.testkit.runner.GradleRunner

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class RemoteSignIntegrationSpec extends Specification {
    File testProjectDir
    File buildFile

    @Shared
    private Javalin javalin

    def setup() {
        javalin = Javalin.create().start(7523)

        testProjectDir = File.createTempDir()
        buildFile = new File(testProjectDir, "build.gradle")
        buildFile << """
            plugins {
                id 'java'
                id 'me.modmuss50.remotesign'
                id 'maven-publish'
            }
        """
        buildFile << """
            remoteSign {
                requestUrl = "http://localhost:7523/sign"
                pgpAuthKey = "pgp"
                jarAuthKey = "jar"
            }
        """
    }

    def cleanup() {
        javalin.stop()
    }

    def signJar() {
        buildFile << """
            remoteSign {
                sign jar
            }
        """

        def requested = false
		int requests = 0;
        given:
            javalin.post("/sign") {
				requests++
				if (requests == 1) {
					// Fail on the first request, it should retry
					it.status(500)
					return
				}

				requested = true
                it.result("Success")
            }
        when:
            def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('build', 'signJar', '--stacktrace')
                .withPluginClasspath()
                .build()
        then:
            result.task(":build").outcome == SUCCESS
            result.task(":signJar").outcome == SUCCESS
            requested
			requests == 2
    }

    def signPublication() {
        buildFile << """
            publishing {
                publications {
                    maven(MavenPublication) {
                        groupId = 'org.gradle.sample'
                        artifactId = 'library'
                        version = '1.1'
            
                        from components.java
                    }
                }
                repositories {
                    maven {
                        url = layout.buildDirectory.dir('repo')
                    }
                }
            }
        """

        buildFile << """
            remoteSign {
                sign publishing.publications.maven
            }
        """

        def requested = false;
        given:
        javalin.post("/sign") {
            requested = true
            it.result("Success")
        }
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('build', 'publish', '--stacktrace', '--configuration-cache')
                .withPluginClasspath()
                .build()
        then:
        result.task(":publish").outcome == SUCCESS
        requested
    }
}
