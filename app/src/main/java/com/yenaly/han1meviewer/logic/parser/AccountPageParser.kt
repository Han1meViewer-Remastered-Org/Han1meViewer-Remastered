package com.yenaly.han1meviewer.logic.parser

import com.yenaly.han1meviewer.logic.model.UserAccount
import com.yenaly.han1meviewer.logic.state.WebsiteState
import org.jsoup.nodes.Element

internal fun parseCsrfToken(parseBody: Element): String {
    return parseBody.selectFirst(HtmlSelectors.CSRF_TOKEN)?.attr("value")
        ?: throw com.yenaly.han1meviewer.logic.exception.ParseException("Can't find csrf token from login page.")
}

internal fun userAccountPage(parseBody: Element): WebsiteState<UserAccount> {
    extractFormError(parseBody)?.let { errorMessage ->
        return WebsiteState.Error(IllegalStateException(errorMessage))
    }

    val csrfToken = parseBody.selectFirst(HtmlSelectors.META_CSRF)?.attr("content")
        ?: parseBody.selectFirst(HtmlSelectors.CSRF_TOKEN)?.attr("value")
    val avatarElement = parseBody.selectFirst("img#playlist-avatar")
    val avatarUrl = avatarElement?.absUrl("src")?.ifBlank { avatarElement.attr("src") }
        .throwIfParseNull("userAccountPage", "avatarUrl")
    val username = parseBody.selectFirst("input[name=name]")?.attr("value")?.trim()
        .throwIfParseNull("userAccountPage", "username")
    val email = parseBody.selectFirst("input[name=email]")?.attr("value")?.trim()
        .throwIfParseNull("userAccountPage", "email")
    val userIdText = parseBody.selectFirst("div.profile-sub-stats-id")?.text()
        .throwIfParseNull("userAccountPage", "userId")
    val userId = Regex("""\d+""").find(userIdText)?.value
        .throwIfParseNull("userAccountPage", "userId")
    val joinedLabel = parseBody.selectFirst("#user-modal-created")?.text()?.trim()
    val statsText = parseBody.selectFirst("div.profile-sub-stats-new-line")?.text().orEmpty()
    val statNumbers = Regex("""\d+""").findAll(statsText)
        .mapNotNull { it.value.toIntOrNull() }
        .toList()

    return WebsiteState.Success(
        UserAccount(
            csrfToken = csrfToken,
            avatarUrl = avatarUrl,
            username = username,
            email = email,
            userId = userId,
            joinedLabel = joinedLabel,
            subscriberCount = statNumbers.getOrElse(0) { 0 },
            videoCount = statNumbers.getOrElse(1) { 0 },
        )
    )
}

