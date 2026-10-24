package com.agrosphere.gateway;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.opentelemetry.api.trace.Span;
@Component @Order(Ordered.LOWEST_PRECEDENCE)
class ActiveTraceFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws IOException,ServletException {
        var context=Span.current().getSpanContext();
        if(context.isValid()) {
            request.setAttribute("gateway.traceId",context.getTraceId());
            request.setAttribute("gateway.spanId",context.getSpanId());
        }
        chain.doFilter(request,response);
    }
}
