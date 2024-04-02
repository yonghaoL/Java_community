package com.learn.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling //启用spring多线程池（默认不启用）,同时让Scheduling注解生效
@EnableAsync //让Async注解生效
public class ThreadPoolConfig {
}
