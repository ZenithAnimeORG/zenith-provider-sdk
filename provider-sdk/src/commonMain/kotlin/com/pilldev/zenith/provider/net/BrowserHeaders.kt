package com.pilldev.zenith.provider.net

public object BrowserHeaders {
    public const val CHROME_DESKTOP_UA: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"

    public const val FIREFOX_DESKTOP_UA: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0"

    public const val CHROME_MOBILE_UA: String =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"

    public const val ACCEPT_HTML: String =
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"

    public const val ACCEPT_JSON: String =
        "application/json, text/plain, */*"

    public const val ACCEPT_XHR: String =
        "application/json, text/javascript, */*; q=0.01"

    public fun standardHeaders(
        referer: String? = null,
        userAgent: String = CHROME_DESKTOP_UA,
        accept: String = ACCEPT_HTML,
    ): Map<String, String> =
        buildMap {
            put("User-Agent", userAgent)
            put("Accept", accept)
            put("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            if (referer != null) {
                put("Referer", referer)
            }
        }
}
