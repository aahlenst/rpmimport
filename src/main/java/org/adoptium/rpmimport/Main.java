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

import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Model;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParameterException;
import static picocli.CommandLine.Parameters;
import static picocli.CommandLine.Spec;

/**
 * @author Andreas Ahlenstorf
 */
@Command(name = "rpmimport", mixinStandardHelpOptions = true, version = "rpmimport 1.0.0",
	description = "Signs and imports RPM packages into a RPM repository")
public class Main implements Callable<Integer> {

	@Parameters(index = "0", description = "Directory where the RPMs to import are stored.")
	File sourceDir;

	@Parameters(index = "1", description = "Directory where the RPM repository is stored.")
	File repositoryDir;

	@Option(names = {"-k", "--key-id"}, description = "ID of GPG key to use for signing.")
	String keyId;

	@Option(names = {"-g", "--gpg-home"}, description = "GPG keychain to use.")
	File gpgHome;

	@Option(names = {"--no-sign"}, negatable = true, description = "GPG keychain to use.")
	boolean sign = true;

	@Spec
	Model.CommandSpec spec;

	public static void main(String... args) {
		int exitCode = new CommandLine(new Main()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception {
		File[] rpms = this.sourceDir.listFiles(pathname -> pathname.getName().endsWith(".rpm"));
		if (rpms == null) {
			throw new ParameterException(this.spec.commandLine(), "Source path is not a directory");
		}
		if (!this.repositoryDir.isDirectory()) {
			throw new ParameterException(this.spec.commandLine(), "Repository path is not a directory");
		}

		RpmSign rpmSign = new RpmSign(keyId, gpgHome);
		Path temporaryDirectory = Files.createTempDirectory("rpmimport");
		for (File rpm : rpms) {
			Path temporaryRpm = Paths.get(temporaryDirectory.toString(), rpm.getName());
			Path finalRpm = Paths.get(this.repositoryDir.toString(), rpm.getName());

			if (finalRpm.toFile().exists()) {
				this.spec.commandLine().getErr().println("File already exists in repository, ignoring: " + finalRpm);
				continue;
			}

			Files.copy(rpm.toPath(), temporaryRpm);

			try {
				if (this.sign) {
					rpmSign.sign(temporaryRpm.toFile());
				}
			} catch (RpmSignException e) {
				// Error is printed to stdout/stderr by RpmSign
				Files.deleteIfExists(temporaryRpm);
				continue;
			}

			Files.move(temporaryRpm, finalRpm, StandardCopyOption.ATOMIC_MOVE);
			Files.deleteIfExists(rpm.toPath());
		}
		return 0;
	}
}
