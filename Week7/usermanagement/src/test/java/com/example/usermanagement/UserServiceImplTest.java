package com.example.usermanagement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.cache.UserCacheManager;
import com.example.usermanagement.entity.Role;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.entity.UserRole;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.mapper.RoleMapper;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.mapper.UserRoleMapper;
import com.example.usermanagement.service.impl.UserServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // 启用Mockito扩展
public class UserServiceImplTest {

	@Mock
	private UserMapper userMapper; // 模拟Mapper

	@Mock
	private RoleMapper roleMapper;

	@Mock
	private UserRoleMapper userRoleMapper;

    @Mock
    private UserCacheManager userCacheManager;

	@InjectMocks
	private UserServiceImpl userService; // 被测试对象，Mock对象会被注入

    private User sampleUser; // 用于测试的用户数据

    @BeforeEach
    void setUp() {
        // 准备一个测试用户数据
        sampleUser = new User();
        sampleUser.setId(1);
        sampleUser.setUsername("alice");
        sampleUser.setEmail("test@example.com");
        sampleUser.setPassword("encodedPassword");
        sampleUser.setAge(25);
    }

    private void mockCachePassThrough() {
        // 纯Mockito场景下，让缓存层透传到dbLoader，聚焦验证UserServiceImpl逻辑
        when(userCacheManager.queryUserById(anyInt(), any())).thenAnswer(invocation -> {
            Supplier<User> dbLoader = invocation.getArgument(1);
            return dbLoader.get();
        });
    }

	@Test
    @DisplayName("getById正常：返回用户并填充角色")
	void getByIdShouldReturnUserWithRolesWhenUserExists() {
        mockCachePassThrough();

		UserRole userRole = new UserRole();
		userRole.setUserId(1);
		userRole.setRoleId(2);

		Role role = new Role();
		role.setId(2);
		role.setName("ROLE_USER");

        // 模拟userMapper.selectById(1)，返回user对象
		when(userMapper.selectById(1)).thenReturn(sampleUser);
        // 模拟角色查询
		when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));
		when(roleMapper.selectBatchIds(List.of(2))).thenReturn(List.of(role));
        // 执行
		User result = userService.getById(1);
        // 断言
		assertEquals(1, result.getId());
		assertEquals("alice", result.getUsername());
		assertEquals(1, result.getRoles().size());
		assertEquals("ROLE_USER", result.getRoles().get(0).getName());

        // 验证 Mapper 方法被调用
        verify(userMapper).selectById(1);
        verify(userRoleMapper).selectList(any());
	}

	@Test
    @DisplayName("getById异常：用户不存在时抛出BusinessException")
	void getByIdShouldThrowBusinessExceptionWhenUserDoesNotExist() {
        mockCachePassThrough();

		when(userMapper.selectById(999)).thenReturn(null);

	BusinessException exception = assertThrows(BusinessException.class, () -> userService.getById(999));
	assertEquals(404, exception.getCode());
	assertTrue(exception.getMessage().contains("用户不存在"));
                
		verify(userRoleMapper, never()).selectList(any(LambdaQueryWrapper.class));
	}

	@Test
    @DisplayName("getById正常：用户没有角色记录时返回空角色列表")
	void getByIdShouldSetEmptyRolesWhenUserHasNoUserRoleRecords() {
        mockCachePassThrough();

		User user = new User();
		user.setId(2);
		user.setUsername("bob");

		when(userMapper.selectById(2)).thenReturn(user);
		when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

		User result = userService.getById(2);

		assertEquals(2, result.getId());
		assertTrue(result.getRoles().isEmpty());
		verify(roleMapper, never()).selectBatchIds(any());
	}

    @Test
    @DisplayName("pageQuery正常分页无关键词：应返回分页数据并填充角色")
    void pageQueryShouldReturnPageWithoutKeyword() {
        UserRole userRole = new UserRole();
        userRole.setUserId(1);
        userRole.setRoleId(2);

        Role role = new Role();
        role.setId(2);
        role.setName("ROLE_USER");

        Page<User> pageResult = new Page<>(1, 10);
        pageResult.setTotal(1);
        pageResult.setRecords(List.of(sampleUser));

        // 模拟userMapper.selectPage，返回分页对象
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);
        // 模拟角色查询
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));
        when(roleMapper.selectBatchIds(anyList())).thenReturn(List.of(role));
        
        // 执行
        Page<User> result = userService.pageQuery(1, 10, null);

        // 断言
        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("alice", result.getRecords().get(0).getUsername());
        assertEquals(1, result.getRecords().get(0).getRoles().size());
        assertEquals("ROLE_USER", result.getRecords().get(0).getRoles().get(0).getName());

        // 验证 Mapper 方法被调用
        verify(userMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        verify(userRoleMapper).selectList(any(LambdaQueryWrapper.class));
        verify(roleMapper).selectBatchIds(anyList());
    }

    @Test
    @DisplayName("pageQuery有关键词：应调用 like 查询")
    void pageQueryShouldSearchByKeyword() {
        Page<User> pageResult = new Page<>(1, 10);
        pageResult.setTotal(1);
        pageResult.setRecords(List.of(sampleUser));

        // 模拟userMapper.selectPage，返回分页对象
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);
        // 模拟该用户没有角色记录
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        // 执行
        Page<User> result = userService.pageQuery(1, 10, "ali");

        // 断言
        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("alice", result.getRecords().get(0).getUsername());
        assertTrue(result.getRecords().get(0).getRoles().isEmpty());

        // 无法直接断言wrapper内容-->验证Mapper方法被调用
        verify(userMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        verify(userRoleMapper).selectList(any(LambdaQueryWrapper.class));
        verify(roleMapper, never()).selectBatchIds(anyList());
    }

}
