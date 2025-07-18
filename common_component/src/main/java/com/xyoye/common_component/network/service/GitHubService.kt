package com.xyoye.common_component.network.service

import com.xyoye.common_component.network.bean.GitHubReleaseBean
import com.xyoye.common_component.network.config.HeaderKey
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * GitHub API服务接口
 * Created by xyoye on 2025/7/18.
 */

interface GitHubService {
    
    /**
     * 获取仓库的所有releases
     * @param baseUrl 动态设置的GitHub API基础URL
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param page 页码，默认为1
     * @param perPage 每页数量，默认为30
     */
    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Header(HeaderKey.BASE_URL) baseUrl: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GitHubReleaseBean>
    
    /**
     * 获取仓库的最新release
     * @param baseUrl 动态设置的GitHub API基础URL
     * @param owner 仓库所有者
     * @param repo 仓库名称
     */
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Header(HeaderKey.BASE_URL) baseUrl: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubReleaseBean
    
    /**
     * 根据tag获取特定的release
     * @param baseUrl 动态设置的GitHub API基础URL
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param tag 标签名称
     */
    @GET("repos/{owner}/{repo}/releases/tags/{tag}")
    suspend fun getReleaseByTag(
        @Header(HeaderKey.BASE_URL) baseUrl: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("tag") tag: String
    ): GitHubReleaseBean
}
