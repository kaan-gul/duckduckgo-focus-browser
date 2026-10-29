/*
 * Copyright (c) 2026 Kaan Gül
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.focusmode.impl.adskip

import android.webkit.WebView
import androidx.core.net.toUri
import com.duckduckgo.app.global.model.Site
import com.duckduckgo.browser.api.JsInjectorPlugin
import com.duckduckgo.di.scopes.AppScope
import com.duckduckgo.feature.toggles.api.Toggle
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import javax.inject.Inject

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class YoutubeAdSkipperPlugin @Inject constructor() : JsInjectorPlugin {

    override fun onPageStarted(
        webView: WebView,
        url: String?,
        isDesktopMode: Boolean?,
        activeExperiments: List<Toggle>,
    ) = Unit

    // YouTube is a single-page app: injecting once per full page load is enough, the guard in the script stops duplicate timers.
    override fun onPageFinished(webView: WebView, url: String?, site: Site?) {
        if (!YoutubeAdSkipperScript.isYoutube(url?.toUri()?.host)) return
        webView.evaluateJavascript(YoutubeAdSkipperScript.SOURCE, null)
    }
}

internal object YoutubeAdSkipperScript {

    fun isYoutube(host: String?): Boolean = host != null && (host == "youtube.com" || host.endsWith(".youtube.com"))

    const val SOURCE = """
        (function() {
            if (window.__focusAdSkipper) return;
            window.__focusAdSkipper = setInterval(function() {
                var skipButton = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button');
                if (skipButton) { skipButton.click(); }

                var adVideo = document.querySelector('.ad-showing video');
                if (adVideo && isFinite(adVideo.duration)) { adVideo.currentTime = adVideo.duration; }
            }, 1000);
        })();
    """
}
