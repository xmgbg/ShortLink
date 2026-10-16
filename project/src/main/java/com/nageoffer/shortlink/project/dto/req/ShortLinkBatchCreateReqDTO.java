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

package com.nageoffer.shortlink.project.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 短链接批量创建请求对象
 */
@Data
public class ShortLinkBatchCreateReqDTO {

    /**
     * 原始链接集合
     */
    @NotEmpty
    @Size(max = 100)
    private List<@NotBlank @Size(max = 2048) String> originUrls;

    /**
     * 描述集合
     */
    @Size(max = 100)
    private List<@Size(max = 1024) String> describes;

    /**
     * 分组标识
     */
    @NotBlank
    @Size(max = 64)
    private String gid;

    /**
     * 创建类型 0：接口创建 1：控制台创建
     */
    @NotNull
    @Min(0)
    @Max(1)
    private Integer createdType;

    /**
     * 有效期类型 0：永久有效 1：自定义
     */
    @NotNull
    @Min(0)
    @Max(1)
    private Integer validDateType;

    /**
     * 有效期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validDate;
}
