package com.houseleasing.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.houseleasing.activiti.WorkflowService;
import com.houseleasing.entity.House;
import com.houseleasing.entity.HouseImage;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.HouseImageMapper;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.UserBehaviorMapper;
import com.houseleasing.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

/**
 * HouseServiceImpl 图片同步逻辑单元测试
 */
class HouseServiceImplTest {

    private WorkflowService workflowService;
    private HouseMapper houseMapper;
    private HouseImageMapper houseImageMapper;
    private UserBehaviorMapper userBehaviorMapper;
    private UserMapper userMapper;
    private RedisTemplate<String, Object> redisTemplate;
    private HouseServiceImpl houseService;

    @BeforeEach
    void setUp() {
        workflowService = mock(WorkflowService.class);
        houseMapper = mock(HouseMapper.class);
        houseImageMapper = mock(HouseImageMapper.class);
        userBehaviorMapper = mock(UserBehaviorMapper.class);
        userMapper = mock(UserMapper.class);
        redisTemplate = mock(RedisTemplate.class);

        houseService = new HouseServiceImpl(
                workflowService,
                houseMapper,
                houseImageMapper,
                userBehaviorMapper,
                userMapper,
                new ObjectMapper(),
                redisTemplate
        );
    }

    @Test
    void addHouse_shouldSyncHouseImagesIntoHouseImagesTable() {
        House newHouse = new House();
        newHouse.setImages("[\"/api/uploads/a.jpg\", \"/api/uploads/b.jpg\"]");
        User owner = new User();
        owner.setId(1L);
        owner.setCreditScore(100);
        when(userMapper.selectById(1L)).thenReturn(owner);

        // 模拟数据库插入后返回自增主键
        doAnswer(invocation -> {
            House arg = invocation.getArgument(0);
            arg.setId(100L);
            return 1;
        }).when(houseMapper).insert(any(House.class));

        houseService.addHouse(newHouse, 1L);

        verify(houseImageMapper, times(1)).delete(any());
        ArgumentCaptor<HouseImage> imageCaptor = ArgumentCaptor.forClass(HouseImage.class);
        verify(houseImageMapper, times(2)).insert(imageCaptor.capture());

        List<HouseImage> insertedImages = imageCaptor.getAllValues();
        assertEquals(100L, insertedImages.get(0).getHouseId());
        assertEquals("/api/uploads/a.jpg", insertedImages.get(0).getImageUrl());
        assertEquals(0, insertedImages.get(0).getSort());

        assertEquals(100L, insertedImages.get(1).getHouseId());
        assertEquals("/api/uploads/b.jpg", insertedImages.get(1).getImageUrl());
        assertEquals(1, insertedImages.get(1).getSort());
    }

    @Test
    void updateHouse_shouldRebuildHouseImagesFromUpdatedPersistedData() {
        House existing = new House();
        existing.setId(9L);
        existing.setOwnerId(2L);
        existing.setImages("[\"/api/uploads/old.jpg\"]");

        House updatedPersisted = new House();
        updatedPersisted.setId(9L);
        updatedPersisted.setOwnerId(2L);
        updatedPersisted.setImages("[\"/api/uploads/new.jpg\"]");

        when(houseMapper.selectById(9L)).thenReturn(existing, updatedPersisted);

        House updateRequest = new House();
        updateRequest.setTitle("新标题");

        houseService.updateHouse(9L, updateRequest, 2L);

        verify(houseImageMapper, times(1)).delete(any());
        ArgumentCaptor<HouseImage> imageCaptor = ArgumentCaptor.forClass(HouseImage.class);
        verify(houseImageMapper, times(1)).insert(imageCaptor.capture());

        HouseImage inserted = imageCaptor.getValue();
        assertEquals(9L, inserted.getHouseId());
        assertEquals("/api/uploads/new.jpg", inserted.getImageUrl());
        assertEquals(0, inserted.getSort());
    }
}
