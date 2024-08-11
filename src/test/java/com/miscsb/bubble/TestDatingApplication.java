package com.miscsb.bubble;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class TestDatingApplication {

	public static void main(String[] args) {
		SpringApplication.from(DatingApplication::main).with(TestDatingApplication.class).run(args);
	}

}
