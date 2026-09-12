/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
package com.luokuiai.flovira.example.postgres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.luokuiai.flovira.example")
public class PostgresExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostgresExampleApplication.class, args);
    }
}
