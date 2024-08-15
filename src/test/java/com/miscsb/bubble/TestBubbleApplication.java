package com.miscsb.bubble;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class TestBubbleApplication {

	public static void main(String[] args) {
		SpringApplication.from(BubbleApplication::main).with(TestBubbleApplication.class).run(args);
	}

}
