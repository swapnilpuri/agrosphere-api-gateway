package com.agrosphere.gateway;
import java.io.IOException;
import java.util.HashMap;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
// Per-process fixed minute window, keyed only by verified JWT subject.
final class SubjectRateLimit extends OncePerRequestFilter {
    private final int limit;
    private final HashMap<String,Integer> counts = new HashMap<>();
    private long minute = -1;
    SubjectRateLimit(int limit) { if (limit < 1) throw new IllegalArgumentException("Rate limit must be positive"); this.limit=limit; }
    private synchronized boolean allow(String subject) {
        long now=System.currentTimeMillis()/60000;
        if(now!=minute){counts.clear();minute=now;}
        if(!counts.containsKey(subject) && counts.size()>=10000) return false;
        int count=counts.getOrDefault(subject,0); if(count>=limit)return false;
        counts.put(subject,count+1); return true;
    }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(req.getRequestURI().startsWith("/api/") && auth instanceof JwtAuthenticationToken jwt && !allow(jwt.getToken().getSubject())) {
            res.setHeader("Retry-After",Long.toString(60-System.currentTimeMillis()/1000%60));
            GatewayProblem.write(res,429,"Request limit exceeded"); return;
        }
        chain.doFilter(req,res);
    }
}
