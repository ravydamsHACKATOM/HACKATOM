package com.studyflow.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Every API route needs a session, except the ones used to obtain it and the health check. */
@Configuration
public class AuthConfig implements WebMvcConfigurer {

  private final AuthInterceptor interceptor;

  public AuthConfig(AuthInterceptor interceptor) {
    this.interceptor = interceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(interceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/auth/register", "/api/auth/login", "/api/auth/logout", "/api/health");
  }
}
