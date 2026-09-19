package com.example.ddd.user.domain.model.valueobject;

/**
 * 值对象：性别。
 */
public enum Gender {

    /** 未知 */
    UNKNOWN(0),
    /** 男 */
    MALE(1),
    /** 女 */
    FEMALE(2);

    private final int code;

    Gender(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /** 从数字码反查枚举 */
    public static Gender fromCode(Integer code) {
        if (code == null) return UNKNOWN;
        for (Gender g : values()) {
            if (g.code == code) return g;
        }
        return UNKNOWN;
    }
}
