package com.qst.smart_warehousing.service.Impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smart_warehousing.common.AuthConst;
import com.qst.smart_warehousing.common.AuthorizationCodeEnum;
import com.qst.smart_warehousing.entity.AdminUser;
import com.qst.smart_warehousing.entity.Result;
import com.qst.smart_warehousing.mapper.AdminUserMapper;
import com.qst.smart_warehousing.service.AdminUserService;
import com.qst.smart_warehousing.util.RedisClient;
import com.qst.smart_warehousing.util.UserUtil;
import com.qst.smart_warehousing.util.WebContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AdminUserServiceImpl extends ServiceImpl<AdminUserMapper, AdminUser> implements AdminUserService, UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库查询用户信息
        AdminUser adminUser = baseMapper.selectOne(new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username));
        if(adminUser==null){
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        return adminUser;
    }


    // 注入Redis客户端，注意该客户端为自己定义的，而非框架提供的
    @Autowired
    RedisClient redisClient;
    //日志对象
    private Logger logger = LoggerFactory.getLogger(AdminUserServiceImpl.class);
    // 注入认证管理器，用于处理认证逻辑，这里需设置懒加载，避免循环依赖
    @Lazy
    @Autowired
    AuthenticationManager authenticationManager;

    @Override
    public Result doLogin(AdminUser user) {
        Result result = null;
        try {
            // 调用认证管理器进行认证，即调用CustomAuthenticationProvider的authenticate方法进行认证
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername().trim(), user.getPassword().trim()));
            // 认证成功后，从认证对象中获取用户详情，这里的用户详情是AdminUser类型
            AdminUser userInfo = (AdminUser) authentication.getDetails();
            if (userInfo==null) {
                result = Result.error(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER);
                return result;
            }
            // 认证成功后，将用户信息存储到Redis中，key为用户ID，value为用户信息，过期时间为AuthConst.TOKEN_EXPIRED_TIME秒
            // 同时，将token也存储到Redis中，key为token，value为用户信息，过期时间也为AuthConst.TOKEN_EXPIRED_TIME秒
            String token = IdUtil.simpleUUID();
            redisClient.set(AuthConst.USER_TOKEN_HEAD.concat(userInfo.getUserId().toString()),userInfo, AuthConst.TOKEN_EXPIRED_TIME, TimeUnit.SECONDS);
            redisClient.set(token,userInfo, AuthConst.TOKEN_EXPIRED_TIME, TimeUnit.SECONDS);
            result =  Result.ok(Map.of("adminToken",token));
        } catch (BadCredentialsException e) {
            result = Result.error(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER);
        }
        return result;
    }

    @Override
    public Result logout() {
        try {
            // 从请求头中获取token
            String token = WebContextUtil.getRequest().getHeader("Admin-Token");
            //删除token和根据用户id设置的缓存
            redisClient.delete(AuthConst.USER_TOKEN_HEAD.concat(UserUtil.getCurrentUserId().toString()));
            redisClient.delete(token);
        }catch (Exception e){
            logger.info("注销失败",e.getMessage());
        }finally {
            return Result.ok();
        }
    }
}
