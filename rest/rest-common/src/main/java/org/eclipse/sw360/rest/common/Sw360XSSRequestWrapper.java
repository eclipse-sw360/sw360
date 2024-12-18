/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * This class is used to sanitize the input from the user to prevent XSS attacks.
 *
 * @author smruti.sahoo@siemens.com
 */
public class Sw360XSSRequestWrapper extends HttpServletRequestWrapper {

	public Sw360XSSRequestWrapper(HttpServletRequest request) {
		super(request);
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		ServletInputStream originalInputStream = super.getInputStream();
		String requestBody = new String(originalInputStream.readAllBytes());

		JsonNode requestBodyJSON = sanitizeInput(new ObjectMapper().readTree(requestBody));
		String sanitizedBody = requestBodyJSON.toString();
		return new ServletInputStream() {
			private final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
					sanitizedBody.getBytes()
			);

			@Override
			public int read() throws IOException {
				return byteArrayInputStream.read();
			}

			@Override
			public boolean isFinished() {
				return byteArrayInputStream.available() == 0;
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setReadListener(ReadListener readListener) {
			}
		};
	}

	@Override
	public String[] getParameterValues(String parameter) {
		String[] values = super.getParameterValues(parameter);
		if (values == null) {
			return null;
		}
		int count = values.length;
		String[] encodedValues = new String[count];
		for (int i = 0; i < count; i++) {
			encodedValues[i] = stripXSS(values[i]);
		}
		return encodedValues;
	}

	@Override
	public String getParameter(String parameter) {
		String value = super.getParameter(parameter);
		return stripXSS(value);
	}

	@Override
	public String getHeader(String name) {
		String value = super.getHeader(name);
		return stripXSS(value);
	}

	private String stripXSS(String value) {
		return org.owasp.encoder.Encode.forHtml(value);

	}

	private JsonNode sanitizeInput(JsonNode input) {
		if (input.isTextual()) {
			return JsonNodeFactory.instance.textNode(stripXSS(input.asText()));
		} else if (input.isArray()) {
			ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
			for (JsonNode element : input) {
				arrayNode.add(sanitizeInput(element));
			}
			return arrayNode;
		} else if (input.isObject()) {
			ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
			input.fields().forEachRemaining(entry -> objectNode.set(entry.getKey(), sanitizeInput(entry.getValue())));
			return objectNode;
		} else {
			return input;
		}
	}

}
