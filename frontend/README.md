# VaultLink Frontend

React + Vite frontend for the VaultLink secure file-sharing platform.

## Setup

```bash
npm install
npm run dev
```

Opens at http://localhost:3000. Your Spring Boot backend must be running on
http://localhost:8080 at the same time.

## IMPORTANT — Backend CORS fix required

Your browser will block requests from localhost:3000 to localhost:8080 unless
the backend explicitly allows it. Add this to your Spring Boot project:

Create `src/main/java/com/vaultlink/vaultlink/config/CorsConfig.java`:

```java
package com.vaultlink.vaultlink.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:3000")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
```

Restart your Spring Boot app after adding this file.

## What's included

- `/login`, `/register` — auth pages, store JWT in localStorage on success
- `/dashboard` — protected route: file list, upload, share, delete
- Axios instance (`src/api.js`) auto-attaches the JWT to every request
- Share modal: pick expiry (1hr / 1day / 7days), generates link, copy button

## Folder structure

```
src/
  api.js              - axios instance with auth interceptor
  AuthContext.jsx     - login state (token/userId/email) via localStorage
  App.jsx             - routes
  pages/
    Login.jsx
    Register.jsx
    Dashboard.jsx
  components/
    ShareModal.jsx
    ProtectedRoute.jsx
```
