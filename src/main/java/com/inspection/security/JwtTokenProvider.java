package com.inspection.security;

import com.inspection.config.JwtConfig;
import com.inspection.entity.User;
import com.inspection.repository.UserRepository;
import com.inspection.service.TokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.stream.Collectors;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    private final JwtConfig jwtConfig;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUserId(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Claims claims = Jwts.claims().setSubject(userDetails.getUsername());
        claims.put("userId", user.getId());
        claims.put("roles", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpirationTime());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(UUID.randomUUID().toString())
                .signWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            // 무효화된 토큰인지 확인
            if (tokenService.isTokenInvalidated(token)) {
                logger.error("무효화된 토큰입니다.");
                return false;
            }
            
            Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SignatureException | MalformedJwtException | ExpiredJwtException | 
                 UnsupportedJwtException | IllegalArgumentException e) {
            logger.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token)
            .getBody();
        
        return ((Integer) claims.get("userId")).longValue();
    }
    
    /**
     * 토큰을 무효화합니다.
     * 
     * @param token 무효화할 토큰
     */
    public void invalidateToken(String token) {
        try {
            // 토큰이 유효한지 먼저 확인
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token);
            
            // 유효한 토큰이면 무효화 처리
            tokenService.invalidateToken(token);
            logger.info("토큰이 무효화되었습니다.");
        } catch (Exception e) {
            logger.error("토큰 무효화 실패: {}", e.getMessage());
            throw new RuntimeException("토큰 무효화에 실패했습니다.");
        }
    }
    
    /**
     * 계약 참여자용 토큰을 생성합니다.
     * 
     * @param participantId 참여자 ID
     * @param validityInMilliseconds 토큰 유효 시간(밀리초)
     * @return 생성된 JWT 토큰
     */
    public String createParticipantToken(Long participantId, long validityInMilliseconds) {
        Claims claims = Jwts.claims().setSubject(participantId.toString());
        claims.put("type", "participant"); // 참여자 토큰임을 명시
        
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .setId(UUID.randomUUID().toString())
                .signWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
    
    /**
     * 참여자 토큰으로부터 참여자 ID를 추출합니다.
     * 
     * @param token 검증할 토큰
     * @return 참여자 ID
     * @throws JwtException 토큰이 유효하지 않을 경우
     */
    public Long getParticipantIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        // 참여자 토큰인지 확인
        if (!"participant".equals(claims.get("type"))) {
            throw new JwtException("유효하지 않은 토큰 타입입니다. 참여자 토큰이 아닙니다.");
        }
        
        return Long.parseLong(claims.getSubject());
    }
    
    /**
     * 참여자 토큰의 유효성을 검증합니다.
     * 
     * @param token 검증할 토큰
     * @return 토큰 유효 여부
     */
    public boolean validateParticipantToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // 참여자 토큰인지 확인
            if (!"participant".equals(claims.get("type"))) {
                logger.error("참여자 토큰이 아닙니다.");
                return false;
            }
            
            return true;
        } catch (SignatureException | MalformedJwtException | ExpiredJwtException | 
                 UnsupportedJwtException | IllegalArgumentException e) {
            logger.error("참여자 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }
} 