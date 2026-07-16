package com.qst.smart_warehousing.service;

import com.qst.smart_warehousing.DTO.UserQueryDTO;
import com.qst.smart_warehousing.DTO.WmsUserSaveDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public interface WmsUserService {

    Map<String, Object> getUserPage(UserQueryDTO queryDTO);

    @Transactional(rollbackFor = Exception.class)
    void createUser(WmsUserSaveDTO dto);

    @Transactional(rollbackFor = Exception.class)
    void updateUser(WmsUserSaveDTO dto);

    @Transactional(rollbackFor = Exception.class)
    void deleteUser(Long id);

    @Transactional(rollbackFor = Exception.class)
    void updateUserStatus(Long id, Integer status);

    List<Map<String, Object>> listAllRoles();

    @Transactional(rollbackFor = Exception.class)
    void assignUserRoles(Long userId, List<Long> roleIds);
}
