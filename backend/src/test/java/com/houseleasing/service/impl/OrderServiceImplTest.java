package com.houseleasing.service.impl;

import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.OrderReviewRequest;
import com.houseleasing.entity.Order;
import com.houseleasing.entity.Review;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.ContractMapper;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.OrderMapper;
import com.houseleasing.mapper.ReviewMapper;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.mapper.UserBehaviorMapper;
import com.houseleasing.mq.MessageProducer;
import com.houseleasing.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    private OrderMapper orderMapper;
    private HouseMapper houseMapper;
    private UserMapper userMapper;
    private ReviewMapper reviewMapper;
    private ContractMapper contractMapper;
    private UserBehaviorMapper userBehaviorMapper;
    private MessageProducer messageProducer;
    private MessageService messageService;
    private RedisTemplate<String, Object> redisTemplate;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        houseMapper = mock(HouseMapper.class);
        userMapper = mock(UserMapper.class);
        reviewMapper = mock(ReviewMapper.class);
        contractMapper = mock(ContractMapper.class);
        userBehaviorMapper = mock(UserBehaviorMapper.class);
        messageProducer = mock(MessageProducer.class);
        messageService = mock(MessageService.class);
        redisTemplate = mock(RedisTemplate.class);
        orderService = new OrderServiceImpl(
                orderMapper,
                houseMapper,
                userMapper,
                reviewMapper,
                contractMapper,
                userBehaviorMapper,
                messageProducer,
                messageService,
                redisTemplate
        );
    }

    @Test
    void reviewOrder_shouldInsertReviewAndIncreaseLandlordCreditForFiveStars() {
        Order order = new Order();
        order.setId(1L);
        order.setTenantId(10L);
        order.setLandlordId(20L);
        order.setHouseId(30L);
        order.setStatus("COMPLETED");
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(reviewMapper.selectCount(any())).thenReturn(0L);

        User landlord = new User();
        landlord.setId(20L);
        landlord.setCreditScore(100);
        when(userMapper.selectById(20L)).thenReturn(landlord);

        OrderReviewRequest request = new OrderReviewRequest();
        request.setRating(5);
        request.setContent("很好");

        orderService.reviewOrder(1L, 10L, request);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper, times(1)).insert(reviewCaptor.capture());
        assertEquals(1L, reviewCaptor.getValue().getOrderId());
        assertEquals(5, reviewCaptor.getValue().getRating());
        assertEquals(105, landlord.getCreditScore());
        verify(userMapper, times(1)).updateById(landlord);
    }

    @Test
    void reviewOrder_shouldRejectDuplicateReview() {
        Order order = new Order();
        order.setId(1L);
        order.setTenantId(10L);
        order.setLandlordId(20L);
        order.setHouseId(30L);
        order.setStatus("COMPLETED");
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(reviewMapper.selectCount(any())).thenReturn(1L);

        OrderReviewRequest request = new OrderReviewRequest();
        request.setRating(4);

        assertThrows(BusinessException.class, () -> orderService.reviewOrder(1L, 10L, request));
        verify(reviewMapper, never()).insert(any());
    }

    @Test
    void reviewOrder_shouldRejectInvalidRating() {
        Order order = new Order();
        order.setId(1L);
        order.setTenantId(10L);
        order.setLandlordId(20L);
        order.setHouseId(30L);
        order.setStatus("COMPLETED");
        when(orderMapper.selectById(1L)).thenReturn(order);

        OrderReviewRequest request = new OrderReviewRequest();
        request.setRating(0);

        assertThrows(BusinessException.class, () -> orderService.reviewOrder(1L, 10L, request));
        verify(reviewMapper, never()).insert(any());
    }

    @Test
    void reviewOrder_shouldDeductLandlordCreditForLowRating() {
        Order order = new Order();
        order.setId(1L);
        order.setTenantId(10L);
        order.setLandlordId(20L);
        order.setHouseId(30L);
        order.setStatus("COMPLETED");
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(reviewMapper.selectCount(any())).thenReturn(0L);

        User landlord = new User();
        landlord.setId(20L);
        landlord.setCreditScore(5);
        when(userMapper.selectById(20L)).thenReturn(landlord);

        OrderReviewRequest request = new OrderReviewRequest();
        request.setRating(2);

        orderService.reviewOrder(1L, 10L, request);

        assertEquals(-5, landlord.getCreditScore());
        verify(userMapper, times(1)).updateById(landlord);
    }
}
