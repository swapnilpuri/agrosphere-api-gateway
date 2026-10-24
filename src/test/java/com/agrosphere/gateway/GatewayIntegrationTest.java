package com.agrosphere.gateway;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
class GatewayIntegrationTest {
    static RSAKey key;
    static HttpServer downstream;
    static ExecutorService executor;
    static ConfigurableApplicationContext context;
    static String base, issuer="https://issuer.example.invalid/realms/test";
    static HttpClient http=HttpClient.newHttpClient();
    static volatile String receivedPath, receivedCorrelation, receivedBearer;
    @BeforeAll static void start() throws Exception {
        key=new RSAKeyGenerator(2048).keyID("test-key").generate();
        downstream=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        executor=Executors.newCachedThreadPool(); downstream.setExecutor(executor);
        downstream.createContext("/certs", exchange -> {
            byte[] bytes=new JWKSet(key.toPublicJWK()).toString().getBytes();
            exchange.getResponseHeaders().set("Content-Type","application/json"); exchange.sendResponseHeaders(200,bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        downstream.createContext("/api/", exchange -> {
            receivedPath=exchange.getRequestURI().toString(); receivedCorrelation=exchange.getRequestHeaders().getFirst("X-Correlation-ID");
            receivedBearer=exchange.getRequestHeaders().getFirst("Authorization");
            if(receivedPath.endsWith("/slow"))try{Thread.sleep(500);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
            byte[] bytes="{\"downstream\":true}".getBytes();
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.getResponseHeaders().set("X-Correlation-ID",receivedCorrelation);
            exchange.getResponseHeaders().set("Location","/api/v1/estates/test-id");
            exchange.sendResponseHeaders(exchange.getRequestMethod().equals("POST")?201:200,bytes.length);
            try{exchange.getResponseBody().write(bytes);}finally{exchange.close();}
        }); downstream.start();
        String url="http://127.0.0.1:"+downstream.getAddress().getPort();
        context=new SpringApplication(Application.class).run("--server.port=0","--spring.security.oauth2.resourceserver.jwt.issuer-uri="+issuer,
            "--OIDC_JWK_SET_URI="+url+"/certs","--ESTATE_SERVICE_URL="+url,"--SPATIAL_SERVICE_URL="+url,"--OPERATIONS_SERVICE_URL="+url,
            "--gateway.limits.body-bytes=1024","--GATEWAY_RATE_LIMIT=3","--GATEWAY_READ_TIMEOUT=100ms");
        base="http://127.0.0.1:"+((WebServerApplicationContext)context).getWebServer().getPort();
    }
    @AfterAll static void stop(){if(context!=null)context.close(); if(downstream!=null)downstream.stop(0);if(executor!=null)executor.shutdownNow();}
    static String token(String subject,String audience,String tokenIssuer,Instant expiry,RSAKey signingKey) throws Exception {
        var claims=new JWTClaimsSet.Builder().issuer(tokenIssuer).subject(subject).audience(audience).issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(expiry)).build();
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(),claims);
        jwt.sign(new RSASSASigner(signingKey));return jwt.serialize();
    }
    static String valid() throws Exception{return token(UUID.randomUUID().toString(),"agrosphere-api",issuer,Instant.now().plusSeconds(300),key);}
    static HttpResponse<String> get(String path,String token) throws Exception {
        var request=HttpRequest.newBuilder(URI.create(base+path)); if(token!=null)request.header("Authorization","Bearer "+token);
        return http.send(request.GET().build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void publicProbesAndProtectedRoutes() throws Exception {
        assertEquals(200,get("/actuator/health/readiness",null).statusCode());
        var denied=get("/api/v1/estates",null);assertEquals(401,denied.statusCode());
        assertTrue(denied.headers().firstValue("Content-Type").orElseThrow().startsWith("application/problem+json"));
        UUID.fromString(denied.headers().firstValue("X-Correlation-ID").orElseThrow());
    }
    @Test void allRoutesPreservePathsQueriesAndBearer() throws Exception {
        for(String path:List.of("/api/v1/estates","/api/v1/estates/id/divisions","/api/v1/divisions/id/blocks","/api/v1/blocks/id","/api/v1/rubber-clones","/api/v1/spatial/features/id","/api/v1/tapping/schedules","/api/v1/tapping/assignments/id","/api/v1/tapping/summary")) {
            String bearer=valid(); var response=get(path+"?size=5",bearer);assertEquals(200,response.statusCode(),response.body());
            assertEquals(path+"?size=5",receivedPath);assertEquals("Bearer "+bearer,receivedBearer);
            assertEquals(response.headers().firstValue("X-Correlation-ID").orElseThrow(),receivedCorrelation);
        }
    }
    @Test void correlationAndPostResponseArePreserved() throws Exception {
        String id=UUID.randomUUID().toString();var request=HttpRequest.newBuilder(URI.create(base+"/api/v1/estates"))
            .header("Authorization","Bearer "+valid()).header("X-Correlation-ID",id).header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        var response=http.send(request,HttpResponse.BodyHandlers.ofString());assertEquals(201,response.statusCode());
        assertEquals(id,receivedCorrelation); assertEquals(id,response.headers().firstValue("X-Correlation-ID").orElseThrow());
        assertEquals("/api/v1/estates/test-id",response.headers().firstValue("Location").orElseThrow());
    }
    @Test void invalidCryptographicAndClaimCasesAreDenied() throws Exception {
        for(String bearer:List.of("unsigned",token("test","wrong",issuer,Instant.now().plusSeconds(300),key),
            token("test","agrosphere-api","https://other.invalid",Instant.now().plusSeconds(300),key),
            token("test","agrosphere-api",issuer,Instant.now().minusSeconds(120),key),
            token("test","agrosphere-api",issuer,Instant.now().plusSeconds(300),new RSAKeyGenerator(2048).generate())))
            assertEquals(401,get("/api/v1/estates",bearer).statusCode());
    }
    @Test void corsAllowsOnlyConfiguredAngularOrigin() throws Exception {
        for(String origin:List.of("http://localhost:4200","https://evil.invalid")) {
            var response=http.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/estates")).header("Origin",origin)
                .header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","authorization,content-type")
                .method("OPTIONS",HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(origin.contains("localhost")?200:403,response.statusCode());
        }
    }
    @Test void authenticatedSubjectsHaveIndependentRateBudgets() throws Exception {
        String bearer=valid();for(int i=0;i<3;i++)assertEquals(200,get("/api/v1/estates",bearer).statusCode());
        var denied=get("/api/v1/estates",bearer);assertEquals(429,denied.statusCode()); assertTrue(denied.headers().firstValue("Retry-After").isPresent());
        assertEquals(200,get("/api/v1/estates",valid()).statusCode());
    }
    @Test void sizedAndStreamingBodiesAreBounded() throws Exception {
        for(boolean streaming:List.of(false,true)) {
            byte[] bytes=new byte[1025];
            var publisher=streaming?HttpRequest.BodyPublishers.ofInputStream(()->new java.io.ByteArrayInputStream(bytes)):HttpRequest.BodyPublishers.ofByteArray(bytes);
            var response=http.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/estates"))
                .header("Authorization","Bearer "+valid()).POST(publisher).build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(413,response.statusCode());assertTrue(response.body().contains("Payload Too Large"));
        }
    }
    @Test void downstreamReadTimeoutIsSafeProblem() throws Exception {
        var response=get("/api/v1/estates/slow",valid());assertEquals(503,response.statusCode(),response.body());
        assertTrue(response.body().contains("Downstream service unavailable"));assertFalse(response.body().contains("Exception"));
    }
}
