/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.codelabs.paging.data.api

import retrofit2.http.GET
import retrofit2.http.Query

const val IN_QUALIFIER = "in:name,description"

/**
 * Github API communication setup via Retrofit.
 */
interface GithubService {
    /**
     * Get repos ordered by stars.
     * https://api.github.com/search/repositories?sort=stars&q=game&page=4&per_page=200
     */
    @GET("search/repositories?sort=stars")
    suspend fun searchRepos(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") itemsPerPage: Int
    ): RepoSearchResponse

//    companion object {
//        private const val BASE_URL = "https://api.github.com/"
//
//        fun create(): GithubService {
//            val logger = HttpLoggingInterceptor()
//            logger.level = Level.BASIC
//
//            val client = OkHttpClient.Builder()
//                .addInterceptor(logger)
//                .build()
//            return Retrofit.Builder()
//                .baseUrl(BASE_URL)
//                .client(client)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build()
//                .create(GithubService::class.java)
//        }
//    }
}
