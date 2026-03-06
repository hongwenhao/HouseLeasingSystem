package com.houseleasing.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String phone;
    private String email;
    private String avatar;
    private String username;
}
