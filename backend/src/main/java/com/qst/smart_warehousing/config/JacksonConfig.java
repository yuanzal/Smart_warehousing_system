package com.qst.smart_warehousing.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 自定义序列化逻辑
            builder.serializerByType(Long.class, new JsonSerializer<Long>() {
                @Override
                public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                    if (value == null) {
                        gen.writeNull();
                        return;
                    }
                    // 💡 关键判定：通过字段名判断，只有字段名包含 "id" 或 "Id" 时才转 String
                    String currentName = gen.getOutputContext().getCurrentName();
                    if (currentName != null && (currentName.toLowerCase().contains("id"))) {
                        gen.writeString(value.toString());
                    } else {
                        // 数量、金额、时间戳等，依然保持 Long（数字）输出
                        gen.writeNumber(value);
                    }
                }
            });
        };
    }
}