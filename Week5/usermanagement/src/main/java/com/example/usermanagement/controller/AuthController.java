package com.example.usermanagement.controller;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.usermanagement.common.Result;
import com.example.usermanagement.dto.LoginDTO;
import com.example.usermanagement.dto.RegisterDTO;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.security.JwtUtil;
import com.example.usermanagement.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    // Request / Post?
    @RequestMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO loginDTO){
        // 执行认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 获取用户完整信息（包含角色）
        User user = userService.findByUsername(userDetails.getUsername());
        // 将角色列表拼成字符串存入token(ROLE_ADMIN,ROLE_USER)
        String roles=user.getRoles()==null?"":user.getRoles().stream().map(role -> role.getName()).collect(java.util.stream.Collectors.joining(","));

        // 生成 JWT token，包含用户 id、用户名和角色信息
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roles);
        return Result.success(token);
    }

    @RequestMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterDTO registerDTO){
        User user=new User();
        BeanUtils.copyProperties(registerDTO, user);
        User created = userService.register(user);
        // 注册成功后返回用户信息（except password）
        created.setPassword(null);
        return Result.success(created);
    }

    @PostMapping("/refresh")
    public Result<String> refresh(@RequestHeader("Authorization") String authHeader){
        // 剩余时间小于10分钟则刷新token
        if(authHeader==null || !authHeader.startsWith("Bearer ")){
            return Result.error(400, "Invalid token format");
        }
        String oldToken=authHeader.substring(7);

        // 1. 校验旧 token 是否有效（未过期且签名正确）
        if (!jwtUtil.validateToken(oldToken)) {
            return Result.error(401, "Invalid or expired token");
        }

        // 2. 判断剩余有效期是否小于10分钟（600000毫秒）
        long thresholdMillis = 600000;
        if(jwtUtil.isTokenExpiringSoon(oldToken, thresholdMillis)){
            // 剩余时间不足10分钟，刷新 token
            String newToken = jwtUtil.refreshToken(oldToken);
            return Result.success(newToken);
        } else{
            // 剩余时间充足，不需要刷新， 返回原Token（或错误提示）
            return Result.error(400, "Token still valid, refresh not needed");
        }
    }
}
