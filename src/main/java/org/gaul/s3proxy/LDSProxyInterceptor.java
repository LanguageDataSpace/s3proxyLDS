package org.gaul.s3proxy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import com.google.common.base.Strings;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LDSProxyInterceptor implements LDSCustomInterceptorI {
	private final URI backendUrl;
	private final String PROXY_PASS;
	private final String LDS_PROXY_PASSOWRD_HEADER;
	private final String AUTHORIZATION_HEADER = "Authorization";
	private final String METHOD_HEADER = "method";
	private final String OPEN_HEADER = "open";

	public LDSProxyInterceptor(URI backendUrl, String ldsProxyPassword, String ldsProxyPasswordHeader) {
		this.backendUrl = backendUrl;
		this.PROXY_PASS = ldsProxyPassword;
		this.LDS_PROXY_PASSOWRD_HEADER = ldsProxyPasswordHeader;
	}

	@Override
	public void intercept(HttpServletRequest request, HttpServletResponse response) throws IOException {
		System.out.println("LSD Inside intercept: " + backendUrl);
		String token = request.getHeader(AUTHORIZATION_HEADER);
		// printRequest(request);
		if (token == null || !validateTokenWithBackend(token, request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "LDS storage invalid or missing token");
			throw new IOException("Request blocked due to invalid token");
		}
		System.out.println("Request approved");
	}

	private boolean validateTokenWithBackend(String token, HttpServletRequest originalRequest) {
		try {
			URL url = backendUrl.toURL();
			System.out.println("LDS validating with " + url);

			String openData = originalRequest.getHeader(OPEN_HEADER);
			String method = originalRequest.getMethod();

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			connection.setRequestMethod("GET");
			connection.setRequestProperty(AUTHORIZATION_HEADER, token);
			connection.setRequestProperty(METHOD_HEADER, method);

			if (!Strings.isNullOrEmpty(openData)) {
				connection.setRequestProperty(OPEN_HEADER, openData);
			}

			if (!Strings.isNullOrEmpty(LDS_PROXY_PASSOWRD_HEADER) && !Strings.isNullOrEmpty(PROXY_PASS)) {
				connection.setRequestProperty(LDS_PROXY_PASSOWRD_HEADER, PROXY_PASS);
			}

			int responseCode = connection.getResponseCode();
			return responseCode == HttpURLConnection.HTTP_OK;
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("LDS Exception occured. Blocking the request");
		return false;
	}

}
