package com.example.usermanagement.enums;

public enum TaskStatus {
    TODO("TODO"),
    IN_PROGRESS("IN_PROGRESS"),
    IN_REVIEW("IN_REVIEW"),
    DONE("DONE");
    // 常量+属性
    // 每一个枚举常量背后都带一个code String属性，表示任务状态的代码(如TODO的code=“TODO”)
    private final String code;

    TaskStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    // String转枚举
    public static TaskStatus fromCode(String code) {
        for (TaskStatus status : TaskStatus.values()) { // 遍历所有枚举常量
            if (status.getCode().equals(code)) { // 如果code匹配，则返回对应的枚举常量
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid TaskStatus code: " + code);
    }

    // 判断当前状态是否可以转换为目标状态
    public boolean canTransitTo(TaskStatus targetStatus) {
        return switch (this) {
            case TODO -> targetStatus == IN_PROGRESS;
            case IN_PROGRESS -> targetStatus == IN_REVIEW;
            case IN_REVIEW -> targetStatus == DONE;
            case DONE -> false;
        };
    }
}
