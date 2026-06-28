package com.example.usermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "邀请项目成员请求参数")
public class ProjectMemberInviteDTO {
    @NotNull(message = "用户ID不能为空")
    @Positive(message = "用户ID必须为正整数")
    @Schema(description = "被邀请用户ID", example = "3")
    private Integer userId;

    @NotNull(message = "项目角色不能为空")
    @Pattern(regexp = "MEMBER|VIEWER", message = "项目角色只能是MEMBER或VIEWER") // 只允许 MEMBER 或 VIEWER
    @Schema(description = "项目角色，只允许MEMBER或VIEWER", example = "MEMBER")
    private String role;
}
