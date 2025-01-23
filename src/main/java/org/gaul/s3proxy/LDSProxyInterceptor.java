package org.gaul.s3proxy;

import java.io.IOException;
import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LDSProxyInterceptor implements LDSCustomInterceptorI {
	private final URI backendUrl;

	public LDSProxyInterceptor(URI backendUrl) {
		this.backendUrl = backendUrl;
	}

	@Override
	public void intercept(HttpServletRequest request, HttpServletResponse response) throws IOException {
		System.out.println("LSD Inside intercept: " + backendUrl);
		String token = request.getHeader("Authorization");
		if (token == null || !validateTokenWithBackend(token)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "LDS storage invalid or missing token");
			throw new IOException("Request blocked due to invalid token");
		}
		System.out.println("Finished intercept");
	}

	private boolean validateTokenWithBackend(String token) throws IOException {
		System.out.println("LDS validating");
		return false;
	}

}
