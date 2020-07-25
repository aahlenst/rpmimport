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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Andreas Ahlenstorf
 */
class RpmSignTest {

	@Test
	void rpmSuccessfullySigned(@TempDir Path tempDir) throws URISyntaxException, IOException {
		Path rpmToSign = Paths.get(tempDir.toString(), "hello-world-1-1.x86_64.rpm");
		Files.copy(Paths.get(this.getClass().getResource("/hello-world-1-1.x86_64.rpm").toURI()), rpmToSign);

		assertThat(RpmTestUtil.getRpmInfo(rpmToSign.toFile())).containsPattern("Signature\\s+:\\s+\\(none\\)");

		var gpgHome = new File(getClass().getResource("/gpg").toURI());

		var rpmSign = new RpmSign("69787003B0E3CCA160692BE5F9AEB7728A3028C7", gpgHome);
		rpmSign.sign(rpmToSign.toFile());

		// The "Key ID" rpm displays is at the end of the GPG key ID passed to GPG/RpmSign above.
		assertThat(RpmTestUtil.getRpmInfo(rpmToSign.toFile())).contains("Key ID f9aeb7728a3028c7");
	}

	@Test
	void throwsIfRpmDoesNotExist(@TempDir Path tempDir) throws URISyntaxException {
		var gpgHome = new File(getClass().getResource("/gpg").toURI());

		var rpmSign = new RpmSign("69787003B0E3CCA160692BE5F9AEB7728A3028C7", gpgHome);
		var exception = assertThrows(RpmSignException.class, () -> rpmSign.sign(new File("does-not-exist")));

		assertThat(exception.getMessage()).isEqualTo("Signing of RPM failed: does-not-exist");
	}
}
