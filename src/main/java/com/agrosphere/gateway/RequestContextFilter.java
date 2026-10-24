package com.agrosphere.gateway;
import java.io.IOException;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.client.ResourceAccessException;
import org.slf4j.*;
@Component("gatewayRequestContextFilter") @Order(Ordered.HIGHEST_PRECEDENCE)
class RequestContextFilter extends OncePerRequestFilter {
    private static final Logger log=LoggerFactory.getLogger(RequestContextFilter.class);
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String supplied=request.getHeader("X-Correlation-ID"); String correlation;
        try { correlation=UUID.fromString(supplied).toString(); } catch(Exception ex) { correlation=UUID.randomUUID().toString(); }
        final String id=correlation;
        var wrapped=new HttpServletRequestWrapper(request) {
            @Override public String getHeader(String name){return name.equalsIgnoreCase("X-Correlation-ID")?id:super.getHeader(name);}
            @Override public Enumeration<String> getHeaders(String name){return name.equalsIgnoreCase("X-Correlation-ID")?Collections.enumeration(List.of(id)):super.getHeaders(name);}
            @Override public Enumeration<String> getHeaderNames(){var names=Collections.list(super.getHeaderNames()); if(names.stream().noneMatch(n->n.equalsIgnoreCase("X-Correlation-ID")))names.add("X-Correlation-ID"); return Collections.enumeration(names);}
        };
        response.setHeader("X-Correlation-ID",id); MDC.put("correlationId",id); long started=System.nanoTime();
        try {chain.doFilter(wrapped,response);} catch(ServletException | IOException | RuntimeException ex) {
            Throwable cause=ex; boolean downstream=false;
            while(cause!=null){if(cause instanceof ResourceAccessException)downstream=true;cause=cause.getCause();}
            if(!downstream || response.isCommitted())throw ex;
            response.resetBuffer(); GatewayProblem.write(response,503,"Downstream service unavailable");
        } finally {
            log.atInfo().addKeyValue("event","gateway_access").addKeyValue("method",request.getMethod()).addKeyValue("path",request.getRequestURI())
                .addKeyValue("status",response.getStatus()).addKeyValue("durationMs",(System.nanoTime()-started)/1000000)
                .log("Gateway request completed"); MDC.remove("correlationId");
        }
    }
}
