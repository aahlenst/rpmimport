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

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Signs rpms by calling rpmsign.
 *
 * @author Andreas Ahlenstorf
 */
class RpmSign {

	@Nullable
	private final String keyId;

	private final Map<String, String> environment = new LinkedHashMap<>();

	/**
	 * Creates a new instance. All files are signed with the keychain and key as configured in {@code /etc/rpm/macros}
	 * or {@code ~/.rpmmacros}.
	 */
	RpmSign() {
		this(null, null);
	}

	/**
	 * Creates a new instance that signs all files with the specified key.
	 */
	RpmSign(String keyId) {
		this(keyId, null);
	}

	/**
	 * Creates a new instance that signs all files with the specified key and uses they keychain in {@code gpgHome}.
	 */
	RpmSign(@Nullable String keyId, @Nullable File gpgHome) {
		this.keyId = keyId;
		if (gpgHome != null) {
			this.environment.put("GNUPGHOME", gpgHome.getAbsolutePath());
		}
	}

	/**
	 * Signs the given rpm.
	 *
	 * @throws RpmSignException in case of an error
	 */
	void sign(File rpm) {
		var command = new ArrayList<String>();
		command.add("rpmsign");
		if (this.keyId != null) {
			command.add("--key-id");
			command.add(this.keyId);
		}
		command.add("--addsign");
		command.add(rpm.getAbsolutePath());

		try {
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			processBuilder.inheritIO();
			processBuilder.environment().putAll(this.environment);

			if (processBuilder.start().waitFor() != 0) {
				throw new RpmSignException("Signing of RPM failed: " + rpm.getPath());
			}
		} catch (InterruptedException | IOException e) {
			throw new RpmSignException("Signing of RPM failed: " + rpm.getPath(), e);
		}
	}
}
