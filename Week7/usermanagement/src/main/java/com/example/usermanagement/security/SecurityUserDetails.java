package com.example.usermanagement.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.example.usermanagement.entity.Role;
import com.example.usermanagement.entity.User;

import lombok.Getter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SecurityUserDetails, 实现UserDetails接口，封装User对象以供Spring Security使用
 * 通过getAuthorities方法将User的角色转换为GrantedAuthority列表，供权限控制
 */
@Getter
public class SecurityUserDetails implements UserDetails {
    private final User user; // 封装原始User对象，获取用户ID等信息

    public SecurityUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<Role> roles = user.getRoles();
        if (roles == null) return List.of();
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();  
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 账号不过期
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 凭证不过期
    }

    @Override
    public boolean isEnabled() {
        return true; // 账号启用
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 账号未锁定
    }
}
