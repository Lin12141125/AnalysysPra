package com.example.usermanagement.security;

import org.springframework.stereotype.Component;

import com.example.usermanagement.exception.BusinessException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 生成token(包含用户id和角色)
    public String generateToken(Integer userId, String username, String roles){
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("roles", roles);
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 从token中提取用户名(subject)
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Integer getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("userId", Integer.class);
    }

    public String getRolesFromToken(String token) {
        final Claims claims = getAllClaimsFromToken(token);
        return claims.get("roles", String.class);
    }


    public Boolean validateToken(String token){
        try{
            getAllClaimsFromToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean isTokenExpired(String token) {
        final Date expiration = getClaimFromToken(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    // 刷新token
    public String refreshToken(String oldToken){
        if(!validateToken(oldToken)){
            throw new BusinessException(401, "Token 无效或已过期，无法刷新");
        }
        String username = getUsernameFromToken(oldToken);
        Integer userId = getUserIdFromToken(oldToken);
        String roles = getRolesFromToken(oldToken);
        return generateToken(userId, username, roles);
    }

    // 获取token剩余有效时间（秒）
    public long getRemainingTime(String token) {
        final Date expiration = getClaimFromToken(token, Claims::getExpiration);
        return expiration.getTime() - System.currentTimeMillis();
    }

    // 判断是否需要刷新token（剩余时间小于10分钟）
    public boolean isTokenExpiringSoon(String token, long thresholdMillis) {
        long remainingTime = getRemainingTime(token);
        return remainingTime < thresholdMillis;
    }
}
