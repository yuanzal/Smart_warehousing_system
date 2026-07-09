package com.qst.smartbuildings.config.security;

import com.qst.smartbuildings.common.AuthorizationCodeEnum;
import com.qst.smartbuildings.entity.AdminUser;
import com.qst.smartbuildings.exception.AuthException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 基于AbstractUserDetailsAuthenticationProvider的自定义认证提供者
 */
@Component
public class CustomAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 传入的用户名密码是否与数据库中的用户信息匹配
     */
    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
//        首先判断传入用户信息是否有效
        AdminUser user = null;
        if (userDetails == null) {
            logger.debug("Authentication failed: 用户不存在");
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_ERR);
        }
        if(userDetails.getUsername()==null){
            logger.debug("Authentication failed: 未提供用户名");
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_USERNAME_REQUIRED);
        }if(!(userDetails instanceof AdminUser)){
            logger.debug("Authentication failed: 用户类型错误");
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_ERR);
        }
        // 将UserDetails转换为AdminUser
        user = (AdminUser) userDetails;
        // 获取提交的密码
        String presentedPassword = authentication.getCredentials().toString();
//        校验密码
        if (passwordEncoder.matches(presentedPassword, user.getPassword())) {
            // 将用户详情设置到认证对象中
            authentication.setDetails(user);
        }else{
            logger.debug("Authentication failed: 密码不匹配");
            throw new BadCredentialsException("密码不正确");
        }

    }

    /**
     * 检索用户信息（通过用户名加载用户）
     */
    @Override
    protected UserDetails retrieveUser(String username,
                                       UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
        try {
            // 调用UserDetailsService加载用户信息
            UserDetails loadedUser = userDetailsService.loadUserByUsername(username);

            if (loadedUser == null) {
                throw new InternalAuthenticationServiceException(
                        "UserDetailsService返回null，违反接口约定");
            }
            return loadedUser;
        } catch (UsernameNotFoundException ex) {
            // 用户名不存在异常直接抛出
            throw ex;
        } catch (Exception ex) {
            // 处理其他异常
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex);
        }
    }
}