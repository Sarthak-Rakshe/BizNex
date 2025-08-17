package com.sarthak.BizNex.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private SecretKey secretKey;

    @Value("${app.security.jwt.secret}")
    private String secret;

    @Value("${app.security.jwt.expiration}")
    private long expirationMs;

    @Value("${security.jwt.refresh-expiration}")
    private long refreshExpirationMs;


    @PostConstruct
    public void init()  {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }


    //Generate access token
    public String generateAccessToken(UserDetails userDetails){
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return buildToken(claims, userDetails.getUsername(), expirationMs);
    }

    //Generate refresh token
    public String generateRefreshToken(UserDetails userDetails){
        return buildToken(Collections.emptyMap(), userDetails.getUsername(), refreshExpirationMs);
    }

    //Extract Username from token
    public String extractUsername(String token){
        return parseClaims(token).getPayload().getSubject();
    }

    //Extract roles from token
    public List<String> extractRoles(String token) {
        Object roles = parseClaims(token).getPayload().get("roles");
        if(roles instanceof List<?>){
            return ((List<?>) roles).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    //Validate JWT token
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            return false; // Token is invalid or expired
        }catch (MalformedJwtException e){
            return false;
        }catch (JwtException | IllegalArgumentException e){
            return false; // Token is malformed or invalid
        }
    }

  public boolean isTokenExpired(String token) {
      Date expiration = parseClaims(token).getPayload().getExpiration();
      return expiration.before(new Date());
  }

    public long getExpirationEpochMillis(String token){
        Date expiration = parseClaims(token).getPayload().getExpiration();
        return expiration.getTime();
    }

    //Parse JWT safely
    private Jws<Claims> parseClaims(String token) {
        try{
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
        }catch (ExpiredJwtException e) {
            throw e; // propagate so caller can handle
        } catch (MalformedJwtException e) {
            throw e;
        } catch (JwtException e) {
            throw e; // generic invalid token
        }
    }

    //Build JWT token with claims, username, and expiration

    private String buildToken(Map<String, Object> claims, String username, long expirationMs) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expirationDate)
                .and()
                .signWith(secretKey)
                .compact();
    }

}
