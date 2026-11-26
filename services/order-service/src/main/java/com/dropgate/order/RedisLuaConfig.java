package com.dropgate.order;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class RedisLuaConfig {

    @Bean("createHoldScript")
    public RedisScript<Long> createHoldScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("redis/create_hold.lua")));
        script.setResultType(Long.class);
        return script;
    }

    @Bean("releaseHoldScript")
    public RedisScript<Long> releaseHoldScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("redis/release_hold.lua")));
        script.setResultType(Long.class);
        return script;
    }

    @Bean("confirmHoldScript")
    public RedisScript<Long> confirmHoldScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("redis/confirm_hold.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
