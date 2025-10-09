package com.xsh.trueused.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
// 如果需要链式构建，可取消下一行注释
// import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
// @Builder // 如需要 builder 模式就解除注释
public class LoginRequest {

        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
}