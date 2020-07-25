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

import org.assertj.core.util.Closeables;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.StringJoiner;

/**
 * @author Andreas Ahlenstorf
 */
final class RpmTestUtil {

	private RpmTestUtil() {
		// no instances
	}

	static String getRpmInfo(File rpm) {
		BufferedReader reader = null;
		try {
			var process = new ProcessBuilder().command("rpm", "-qpi", rpm.getAbsolutePath()).start();
			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			StringJoiner joiner = new StringJoiner(System.getProperty("line.separator"));
			reader.lines().iterator().forEachRemaining(joiner::add);
			return joiner.toString();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			Closeables.closeQuietly(reader);
		}
	}
}
