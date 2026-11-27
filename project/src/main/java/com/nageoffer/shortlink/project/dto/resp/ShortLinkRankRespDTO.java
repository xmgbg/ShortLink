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

package com.nageoffer.shortlink.project.dto.resp;

/**
 * 分组热门短链接排行条目
 *
 * @param title        短链接描述
 * @param fullShortUrl 完整短链接地址
 * @param originUrl    原始链接地址
 * @param favicon      原始网站图标地址
 * @param pv           所选时段内的访问次数
 */
public record ShortLinkRankRespDTO(String title, String fullShortUrl, String originUrl, String favicon, long pv) {
}
