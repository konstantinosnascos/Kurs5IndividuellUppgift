package com.example.demo.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("!test")
public class RateLimitFilter implements Filter {

    private final Map<String, Bucket> buckets =
            new ConcurrentHashMap<>();

    private Bucket createNewBucket() {

        Bandwidth limit = Bandwidth.classic(
                50,
                Refill.greedy(2, Duration.ofSeconds(1))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest =
                (HttpServletRequest) request;

        String path = httpRequest.getRequestURI();

        String userAgent = httpRequest.getHeader("User-Agent");

        if (userAgent != null &&
                userAgent.contains("Apache-HttpClient")) {

            chain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")) {

            chain.doFilter(request, response);
            return;
        }

        String ip = httpRequest.getRemoteAddr();

        Bucket bucket =
                buckets.computeIfAbsent(
                        ip,
                        k -> createNewBucket()
                );

        if (bucket.tryConsume(1)) {

            chain.doFilter(request, response);

        } else {

            HttpServletResponse httpResponse =
                    (HttpServletResponse) response;

            httpResponse.setStatus(429);

            httpResponse.getWriter()
                    .write("Too many requests");
        }
    }
}
