package com.bloodconnect.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final List<String> PUBLIC_URLS = Arrays.asList(
        "/login", "/register", "/logout", "/", "/index.html", "/login.html", "/register.html", "/error.html"
    );

    private static final List<String> STATIC_PREFIXES = Arrays.asList(
        "/css/", "/js/", "/images/", "/screenshots/", "/favicon.ico"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        // Allow public URLs
        if (isPublicUrl(path)) {
            return true;
        }

        // Allow static assets
        if (isStaticAsset(path)) {
            return true;
        }

        // Check authentication
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            if (isApiRequest(path)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\": \"Unauthorized. Please log in.\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/login.html");
            }
            return false;
        }

        String role = (String) session.getAttribute("role");

        // Role-based authorization
        boolean authorized = true;
        if (path.startsWith("/admin/") || path.equals("/admin") || path.equals("/admin-dashboard.html")) {
            if (!"ADMIN".equals(role)) {
                authorized = false;
            }
        } else if (path.startsWith("/donor/") || path.equals("/donor") || path.equals("/donor-dashboard.html")) {
            if (!"DONOR".equals(role)) {
                authorized = false;
            }
        } else if (path.startsWith("/request/") || path.equals("/request") || path.equals("/requester-dashboard.html") 
                || path.equals("/request-form.html") || path.equals("/match-results.html") || path.equals("/match/find")) {
            if (!"REQUESTER".equals(role)) {
                authorized = false;
            }
        }

        if (!authorized) {
            if (isApiRequest(path)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\": \"Forbidden. Insufficient permissions.\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/login.html?error=unauthorized");
            }
            return false;
        }

        return true;
    }

    private boolean isApiRequest(String path) {
        return path.equals("/donor/profile") 
            || path.startsWith("/request/") 
            || path.equals("/match/find") 
            || path.startsWith("/admin/")
            || path.equals("/login")
            || path.equals("/register")
            || path.equals("/logout");
    }

    private boolean isPublicUrl(String path) {
        for (String url : PUBLIC_URLS) {
            if (path.equals(url)) {
                return true;
            }
        }
        return false;
    }

    private boolean isStaticAsset(String path) {
        for (String prefix : STATIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return path.equals("/") || path.isEmpty();
    }
}
