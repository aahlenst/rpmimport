/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.adoptium.rpmimport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andreas Ahlenstorf
 */
class MainTest {
	@Test
	void throwsIfSourceIsNotADirectory(@TempDir Path destination) {
		Main app = new Main();
		CommandLine cmd = new CommandLine(app);

		StringWriter stdoutWriter = new StringWriter();
		cmd.setOut(new PrintWriter(stdoutWriter));
		StringWriter stderrWriter = new StringWriter();
		cmd.setErr(new PrintWriter(stderrWriter));

		int exitCode = cmd.execute("does-not-exist", destination.toString());

		assertThat(exitCode).isEqualTo(2);
		assertThat(stderrWriter.toString()).startsWith("Source path is not a directory");
	}

	@Test
	void throwsIfDestinationIsNotADirectory(@TempDir Path source) {
		Main app = new Main();
		CommandLine cmd = new CommandLine(app);

		StringWriter stdoutWriter = new StringWriter();
		cmd.setOut(new PrintWriter(stdoutWriter));
		StringWriter stderrWriter = new StringWriter();
		cmd.setErr(new PrintWriter(stderrWriter));

		int exitCode = cmd.execute(source.toString(), "does-not-exist");

		assertThat(exitCode).isEqualTo(2);
		assertThat(stderrWriter.toString()).startsWith("Repository path is not a directory");
	}

	@Test
	void processesAllRpms(@TempDir Path tempDir) throws Exception {
		var gpgHome = new File(getClass().getResource("/gpg").toURI());
		var source = Paths.get(tempDir.toString(), "source");
		var destination = Paths.get(tempDir.toString(), "destination");

		Files.createDirectory(source);
		Files.createDirectory(destination);

		var helloWorldRpm = Paths.get(this.getClass().getResource("/hello-world-1-1.x86_64.rpm").toURI());
		Files.copy(helloWorldRpm, Paths.get(source.toString(), "hello-world-1-1.x86_64.rpm"));

		var emptyRpm = Paths.get(this.getClass().getResource("/empty-rpm-1-1.x86_64.rpm").toURI());
		Files.copy(emptyRpm, Paths.get(source.toString(), "empty-rpm-1-1.x86_64.rpm"));

		assertThat(source.toFile().list()).containsOnly("empty-rpm-1-1.x86_64.rpm", "hello-world-1-1.x86_64.rpm");
		assertThat(destination.toFile().list()).isEmpty();

		Main app = new Main();
		CommandLine cmd = new CommandLine(app);

		StringWriter stdoutWriter = new StringWriter();
		cmd.setOut(new PrintWriter(stdoutWriter));
		StringWriter stderrWriter = new StringWriter();
		cmd.setErr(new PrintWriter(stderrWriter));

		int exitCode = cmd.execute(
			"--gpg-home", gpgHome.getAbsolutePath(),
			"--key-id", "69787003B0E3CCA160692BE5F9AEB7728A3028C7",
			source.toString(), destination.toString()
		);

		assertThat(exitCode).isEqualTo(0);
		assertThat(source.toFile().list()).isEmpty();
		assertThat(destination.toFile().list())
			.containsOnly("empty-rpm-1-1.x86_64.rpm", "hello-world-1-1.x86_64.rpm");
		assertThat(RpmTestUtil.getRpmInfo(new File(destination.toFile(), "empty-rpm-1-1.x86_64.rpm")))
			.containsPattern("Key ID f9aeb7728a3028c7");
		assertThat(RpmTestUtil.getRpmInfo(new File(destination.toFile(), "hello-world-1-1.x86_64.rpm")))
			.containsPattern("Key ID f9aeb7728a3028c7");
	}

	@Test
	void processesAllRpmsWithoutSigning(@TempDir Path tempDir) throws Exception {
		var source = Paths.get(tempDir.toString(), "source");
		var destination = Paths.get(tempDir.toString(), "destination");

		Files.createDirectory(source);
		Files.createDirectory(destination);

		var helloWorldRpm = Paths.get(this.getClass().getResource("/hello-world-1-1.x86_64.rpm").toURI());
		Files.copy(helloWorldRpm, Paths.get(source.toString(), "hello-world-1-1.x86_64.rpm"));

		var emptyRpm = Paths.get(this.getClass().getResource("/empty-rpm-1-1.x86_64.rpm").toURI());
		Files.copy(emptyRpm, Paths.get(source.toString(), "empty-rpm-1-1.x86_64.rpm"));

		assertThat(source.toFile().list()).containsOnly("empty-rpm-1-1.x86_64.rpm", "hello-world-1-1.x86_64.rpm");
		assertThat(destination.toFile().list()).isEmpty();

		Main app = new Main();
		CommandLine cmd = new CommandLine(app);

		StringWriter stdoutWriter = new StringWriter();
		cmd.setOut(new PrintWriter(stdoutWriter));
		StringWriter stderrWriter = new StringWriter();
		cmd.setErr(new PrintWriter(stderrWriter));

		int exitCode = cmd.execute("--no-sign", source.toString(), destination.toString());

		assertThat(exitCode).isEqualTo(0);
		assertThat(source.toFile().list()).isEmpty();
		assertThat(destination.toFile().list())
			.containsOnly("empty-rpm-1-1.x86_64.rpm", "hello-world-1-1.x86_64.rpm");
		assertThat(RpmTestUtil.getRpmInfo(new File(destination.toFile(), "empty-rpm-1-1.x86_64.rpm")))
			.containsPattern("Signature\\s+:\\s+\\(none\\)");
		assertThat(RpmTestUtil.getRpmInfo(new File(destination.toFile(), "hello-world-1-1.x86_64.rpm")))
			.containsPattern("Signature\\s+:\\s+\\(none\\)");
	}

	@Test
	void skipsFileThatAlreadyExistsAndLeavesItInSourceFolder(@TempDir Path tempDir) throws Exception {
		var source = Paths.get(tempDir.toString(), "source");
		var destination = Paths.get(tempDir.toString(), "destination");

		Files.createDirectory(source);
		Files.createDirectory(destination);

		var helloWorldRpm = Paths.get(this.getClass().getResource("/hello-world-1-1.x86_64.rpm").toURI());
		Files.copy(helloWorldRpm, Paths.get(source.toString(), "hello-world-1-1.x86_64.rpm"));

		var existingRpm = new File(destination.toFile(), "hello-world-1-1.x86_64.rpm");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(existingRpm))) {
			writer.write("Hello");
		}

		assertThat(source.toFile().list()).containsOnly("hello-world-1-1.x86_64.rpm");
		assertThat(destination.toFile().list()).containsOnly("hello-world-1-1.x86_64.rpm");

		Main app = new Main();
		CommandLine cmd = new CommandLine(app);

		StringWriter stdoutWriter = new StringWriter();
		cmd.setOut(new PrintWriter(stdoutWriter));
		StringWriter stderrWriter = new StringWriter();
		cmd.setErr(new PrintWriter(stderrWriter));

		int exitCode = cmd.execute(source.toString(), destination.toString());

		assertThat(exitCode).isEqualTo(0);
		assertThat(stderrWriter.toString()).startsWith("File already exists in repository, ignoring:");
		assertThat(source.toFile().list()).containsOnly("hello-world-1-1.x86_64.rpm");
		assertThat(destination.toFile().list()).containsOnly("hello-world-1-1.x86_64.rpm");
		assertThat(existingRpm).hasContent("Hello");
	}
}
