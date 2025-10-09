package com.xsh.trueused.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空") @Size(min = 3, max = 50, message = "用户名长度需要在3-50之间") String username,

        @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email,

        @NotBlank(message = "密码不能为空") @Size(min = 6, max = 100, message = "密码长度需要在6-100之间") String password) {
}
