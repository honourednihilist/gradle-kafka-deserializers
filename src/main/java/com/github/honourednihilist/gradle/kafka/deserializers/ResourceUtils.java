package com.github.honourednihilist.gradle.kafka.deserializers;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResourceUtils {

	private ResourceUtils() {}

	private static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

	public static String getResourceAsString(String name) {
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		StringBuilder ret = new StringBuilder();
		String line;

		try {
			while ((line = reader.readLine()) != null) ret.append(line).append(LINE_SEPARATOR);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			closeQuietly(reader);
			closeQuietly(inputStream);
		}

		return ret.toString();
	}

	private static void closeQuietly(Closeable closeable) {
		try {
			if (closeable != null) closeable.close();
		} catch (IOException ignored) {}
	}
}
