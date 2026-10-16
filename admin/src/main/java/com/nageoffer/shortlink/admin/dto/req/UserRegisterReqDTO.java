/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nageoffer.shortlink.admin.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求参数
 */
@Data
public class UserRegisterReqDTO {

    /**
     * 用户名
     */
    @NotBlank
    @Size(min = 3, max = 32)
    private String username;

    /**
     * 密码
     */
    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    /**
     * 真实姓名
     */
    @NotBlank
    @Size(max = 64)
    private String realName;

    /**
     * 手机号
     */
    @Pattern(regexp = "^$|^1\\d{10}$")
    private String phone;

    /**
     * 邮箱
     */
    @Email
    @Size(max = 128)
    private String mail;
}
