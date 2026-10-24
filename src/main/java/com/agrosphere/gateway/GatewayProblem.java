package com.agrosphere.gateway;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
final class GatewayProblem {
    private GatewayProblem() {}
    static void write(HttpServletResponse response, int status, String title) throws IOException {
        response.setStatus(status); response.setContentType("application/problem+json");
        response.getWriter().write("{\"type\":\"about:blank\",\"status\":"+status+",\"title\":\""+title+"\",\"correlationId\":\""+response.getHeader("X-Correlation-ID")+"\"}");
    }
}
