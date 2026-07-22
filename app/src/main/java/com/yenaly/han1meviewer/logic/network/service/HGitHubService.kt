package com.yenaly.han1meviewer.logic.network.service

import com.yenaly.han1meviewer.logic.model.github.Artifacts
import com.yenaly.han1meviewer.logic.model.github.CommitComparison
import com.yenaly.han1meviewer.logic.model.github.Release
import com.yenaly.han1meviewer.logic.model.github.WorkflowRuns
import com.yenaly.han1meviewer.logic.network.HUpdater
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/09/09 009 20:04
 */
interface HGitHubService {
    @GET("releases/latest")
    suspend fun getLatestVersion(): Release

    /**
     * 通过 GitHub Contents API 直接获取仓库根目录的 announcements.json 原文。
     *
     * 相比 raw.githubusercontent.com，走 api.github.com（应用已用固定 IP 的
     * [GitHubDns][com.yenaly.han1meviewer.logic.network.GitHubDns] 可靠连通，检查更新即走此通道），
     * 在受限网络下更稳。Accept: application/vnd.github.raw 让响应体即为文件原文。
     */
    @Headers("Accept: application/vnd.github.raw")
    @GET("contents/announcements.json")
    suspend fun getAnnouncements(): Response<ResponseBody>

    /**
     * What is Workflow Runs?
     *
     * List all workflow runs for a repository. You can use parameters to filter the list of results. For example, you
     * can get a list of workflow runs for a specific branch, or you can get a list of workflow runs that used a specific
     * workflow file.
     */
    @GET("actions/workflows/ci.yml/runs?event=push&status=success&per_page=1")
    suspend fun getWorkflowRuns(
        @Query("branch") branch: String = HUpdater.DEFAULT_BRANCH,
    ): WorkflowRuns

    /**
     * What is Commit Comparison?
     *
     * Compare two commits in a repository. The response will include a comparison of the two commits. The response can
     * include difference in various aspects such as files, commits, and comments.
     */
    @GET("compare/{curSha}...{latestSha}")
    suspend fun getCommitComparison(
        @Path("curSha") curSha: String,
        @Path("latestSha") latestSha: String,
    ): CommitComparison

    /**
     * What is Artifacts?
     *
     * Artifacts are the files produced by a workflow run. They are associated with the run during the execution of the
     * job that produces them. Artifacts are available for 90 days after the run is completed.
     */
    @GET
    suspend fun getArtifacts(
        @Url url: String,
    ): Artifacts

    /**
     * Typical request
     */
    @GET
    @Streaming
    suspend fun request(
        @Url url: String,
    ): Response<ResponseBody>
}