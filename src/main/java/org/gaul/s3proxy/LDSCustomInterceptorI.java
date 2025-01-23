package org.gaul.s3proxy;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface LDSCustomInterceptorI {
	void intercept(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
