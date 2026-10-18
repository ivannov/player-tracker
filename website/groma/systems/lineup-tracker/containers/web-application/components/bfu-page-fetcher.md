---
type: C4 Component
title: BFU page fetcher
status: stable
groma:
  id: bfu-page-fetcher
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/BfuHttp.java
      symbol: BfuHttp
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/BfuScraperException.java
      symbol: BfuScraperException
  group: BFU scraping
description: Downloads bfu-tournaments.com pages past Cloudflare
---

Shared by the three bfu-tournaments.com scrapers. Fetches a page with the JDK HttpClient and browser-like headers, then hands the HTML to Jsoup for parsing. Jsoup's own connection is answered with HTTP 403 by the site's Cloudflare front, which is why fetching and parsing are split. Also owns the scraper exception that all BFU scrapers raise.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/scraping/BfuHttp.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/BfuHttp.java) | [bfu-tournaments-site](../../../../../externals/bfu-tournaments-site.md) | Downloads HTML pages | HTTPS, JDK HttpClient, Jsoup |
