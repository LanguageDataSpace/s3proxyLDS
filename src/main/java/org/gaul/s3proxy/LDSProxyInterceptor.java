package org.gaul.s3proxy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.google.common.base.Strings;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LDSProxyInterceptor implements LDSCustomInterceptorI {
	private final URI backendUrl;
	private final String PROXY_PASS;
	private final String LDS_PROXY_PASSOWRD_HEADER;
	private final String LDS_PUBLIC_FOLDER;
	private final String AUTHORIZATION_HEADER = "Authorization";
	private final String METHOD_HEADER = "method";
	private final String OPEN_HEADER = "open";
	private final String PATH_HEADER = "path";

	public LDSProxyInterceptor(URI backendUrl, String ldsProxyPassword, String ldsProxyPasswordHeader,
			String ldsPublicFolder) {
		this.backendUrl = backendUrl;
		this.PROXY_PASS = ldsProxyPassword;
		this.LDS_PROXY_PASSOWRD_HEADER = ldsProxyPasswordHeader;
		this.LDS_PUBLIC_FOLDER = "/" + ldsPublicFolder;
	}

	private String maskToken(String token) {
		if (token == null || token.length() < 8) {
			return "***";
		}
		return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
	}

	@Override
	public void intercept(HttpServletRequest request, HttpServletResponse response) throws IOException {
		System.out.println("LSD Inside intercept: " + backendUrl);
		String token = request.getHeader(AUTHORIZATION_HEADER);
		String cookieToken = getTokenFromCookies(request);
		// printRequest(request);
		// System.out.println("LDS Token is: " + token);
		if (isOpenPath(request)) {
			System.out.println("Open Request approved");
		} else {
			if (token == null) {
				System.out.println("LDS token is null: " + token);
			}
			if (token == null && cookieToken != null) {
				token = cookieToken;
				System.out.println("LDS cookie token: " + maskToken(token));
			}
			if (token == null || !validateTokenWithBackend(token, request)) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "LDS storage invalid or missing token");
				throw new IOException("Request blocked due to invalid token");
			}
			System.out.println("Request approved");
		}
	}

	private boolean isOpenPath(HttpServletRequest originalRequest) {
		String method = originalRequest.getMethod();
		// String requestUrl = originalRequest.getRequestURL().toString();
		String path_info = originalRequest.getPathInfo();
		System.out.println("LDS path info: " + path_info);
		System.out.println("Public folder: " + LDS_PUBLIC_FOLDER);
		if (!method.equalsIgnoreCase("get")) {
			return false;
		}
		if (path_info == null || path_info.trim() == "" || path_info == "/") {
			return false;
		}
		// if (!path_info.startsWith(LDS_PUBLIC_FOLDER)) {
		// return false;
		// }
		String normilizedPath = normalizePath(path_info);
		if (normilizedPath.contains("..")) {
			return false;
		}
		if (normilizedPath.equals(LDS_PUBLIC_FOLDER) && !normilizedPath.startsWith(LDS_PUBLIC_FOLDER + "/")) {
			return false;
		}

		/*
		 * String[] segments = normilizedPath.split("/");
		 * for (int i = 0; i < segments.length; i++) {
		 * if (segments[i].equals(LDS_PUBLIC_FOLDER.replace("/", ""))) {
		 * return true;
		 * }
		 * }
		 */

		return true;
	}

	private boolean validateTokenWithBackend(String token, HttpServletRequest originalRequest) {
		try {
			URL url = backendUrl.toURL();
			System.out.println("LDS validating with " + url);

			String openData = originalRequest.getHeader(OPEN_HEADER);
			String method = originalRequest.getMethod();
			String path_info = originalRequest.getPathInfo();
			System.out.println("Original request url " + path_info);

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			connection.setRequestMethod("GET");
			connection.setRequestProperty(AUTHORIZATION_HEADER, token);
			connection.setRequestProperty(METHOD_HEADER, method);
			connection.setRequestProperty(PATH_HEADER, path_info);

			if (!Strings.isNullOrEmpty(openData)) {
				connection.setRequestProperty(OPEN_HEADER, openData);
			}

			if (!Strings.isNullOrEmpty(LDS_PROXY_PASSOWRD_HEADER) && !Strings.isNullOrEmpty(PROXY_PASS)) {
				connection.setRequestProperty(LDS_PROXY_PASSOWRD_HEADER, PROXY_PASS);
			}

			int responseCode = connection.getResponseCode();

			if (!Strings.isNullOrEmpty(LDS_PROXY_PASSOWRD_HEADER) && !Strings.isNullOrEmpty(PROXY_PASS)) {
				if (connection.getHeaderField(LDS_PROXY_PASSOWRD_HEADER).equals(PROXY_PASS)) {
					return responseCode == HttpURLConnection.HTTP_OK;
				} else {
					System.out.println("LDS response from proxy with incorrect password and header combination");
					return false;
				}
			} else {
				return responseCode == HttpURLConnection.HTTP_OK;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("LDS Exception occured. Blocking the request");
		return false;
	}

	private String getTokenFromCookies(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("Authorization".equals(cookie.getName())) {
					try {
						return URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8.name());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		return null;
	}

	private String normalizePath(String path) {
		try {
			String decoded = java.net.URLDecoder.decode(path, java.nio.charset.StandardCharsets.UTF_8);
			String normalized = decoded.replaceAll("/+", "/");

			return normalized;
		} catch (Exception e) {
			return "";
		}
	}

	private void printRequest(HttpServletRequest request) {
		String method = request.getMethod();
		String contextPath = request.getContextPath();
		String pathInfo = request.getPathInfo();
		String requestUrl = request.getRequestURL().toString();
		String queryString = request.getQueryString();
		System.out.println("method: " + method);
		System.out.println("contextPath: " + contextPath);
		System.out.println("pathInfo: " + pathInfo);
		System.out.println("requestURL: " + requestUrl);
		System.out.println("queryString: " + queryString);
		String localAddr = request.getLocalAddr();
		System.out.println("localAddress: " + localAddr);
		String localName = request.getLocalName();
		System.out.println("localName: " + localName);
		String remoteAddrs = request.getRemoteAddr();
		String remoteHost = request.getRemoteHost();
		int remotePort = request.getRemotePort();
		System.out.println("remote address: " + remoteAddrs);
		System.out.println("remote host: " + remoteHost);
		System.out.println("remote port: " + remotePort);
	}

}
