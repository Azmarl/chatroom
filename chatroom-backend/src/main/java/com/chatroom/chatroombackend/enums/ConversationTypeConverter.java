package com.chatroom.chatroombackend.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 自定义的 JPA 属性转换器，用于处理 ConversationType 枚举与数据库字符串之间的大小写转换。
 * 当从数据库读取时（`convertToEntityAttribute`），它会将小写字符串转换为大写的枚举常量。
 * 当写入数据库时（`convertToDatabaseColumn`），它会将大写的枚举常量转换为小写字符串。
 */
@Converter(autoApply = true)
public class ConversationTypeConverter implements AttributeConverter<ConversationType, String> {

    /**
     * 将 ConversationType 枚举转换为数据库列存储的字符串。
     *
     * @param attribute ConversationType 枚举值
     * @return 数据库中存储的小写字符串，如果枚举值为 null 则返回 null
     */
    @Override
    public String convertToDatabaseColumn(ConversationType attribute) {
        if (attribute == null) {
            return null;
        }
        // 将枚举名转换为小写后存储到数据库
        return attribute.name().toLowerCase();
    }

    /**
     * 将数据库中存储的字符串转换为 ConversationType 枚举。
     *
     * @param dbData 数据库中的小写字符串
     * @return 对应的 ConversationType 枚举，如果字符串为 null 则返回 null
     */
    @Override
    public ConversationType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        // 将数据库中的小写字符串转换为大写，然后获取对应的枚举常量
        return ConversationType.valueOf(dbData.toUpperCase());
    }
}