![Icon](https://github.com/LibreBlogOrg/LibreblogRSS-Resources/blob/main/images/dog_round.webp)

# LibreBlog RSS

*A simple Android RSS reader that feels like a social timeline — supports multimedia, .onion sites, and works well with non-monopolistic platforms (Mastodon, Substack, etc.).*

[![Download APK](https://badgen.net/badge/Download/APK/green?icon=java)](https://github.com/LibreBlogOrg/LibreblogRSS/releases)
[![Changelog](https://badgen.net/badge/CHANGE/LOG/red)](https://github.com/LibreBlogOrg/LibreblogRSS/blob/main/CHANGELOG.md)

## Update - Version 1.1.0

From version 1.1.0 onward, LibreBlog RSS will be ActivityPub‑first. The app will try the public outbox first and fall back to RSS if that fails. This lets users see both original posts and reposts (boosts/shares) from people they follow, so timelines behave more like social networks. Currently supported: Mastodon, Pleroma, and Misskey.

We also render custom emojis, since they are widely used in the Fediverse. Currently supported: Mastodon and Pleroma.

<hr/>
<div style="display:flex;gap:8px;flex-wrap:wrap;">
  <img src="https://github.com/LibreBlogOrg/LibreblogRSS-Resources/blob/main/images/screenshot1.avif" alt="Feed" style="width:250px;">
  <img src="https://github.com/LibreBlogOrg/LibreblogRSS-Resources/blob/main/images/screenshot2.avif" alt="Add source" style="width:250px;">
  <img src="https://github.com/LibreBlogOrg/LibreblogRSS-Resources/blob/main/images/screenshot3.avif" alt="Dark theme" style="width:250px;">
  <img src="https://github.com/LibreBlogOrg/LibreblogRSS-Resources/blob/main/images/screenshot4.avif" alt="Video player" style="width:250px;">
  <img src="https://github.com/LibreBlogOrg/LibreblogRSS-Resources/blob/main/images/screenshot5.avif" alt="Settings" style="width:250px;">
  <img src="https://github.com/LibreBlogOrg/LibreblogRSS-Resources/blob/main/images/screenshot6.avif" alt="More options" style="width:250px;">
</div>
<hr/>

## Features

- Lightweight, customisable recommendation algorithm.
- Custom emojis and Fediverse reposts.
- Light and dark themes.
- Fullscreen video player.
- No ads, no tracking.
- Follow .onion sites (requires Orbot).
- Import/export OPML.
- Copy and paste links — the app will try to find the feed and the profile image.

## (Opinionated) Limitations

- No notifications, no badge counts, no gamification — no anxiety.
- No folders or categorisation — just a flow.
- Posts are deleted after a customisable number of days (unless liked).
- Articles open in the default browser (or in Tor for .onion links).
- The app does not run in the background — posts load only while it's in use.

## Why bother supporting .onion when few news organizations are there?

To make static sites built with this [Editor](https://github.com/LibreBlogOrg/LibreBlog) easily accessible in the app (using OnionShare).

## Who is this app for?

For people done with social platforms who still want to follow things they find interesting.

## Installation

You can use [Obtainium](https://obtainium.imranr.dev) — add this link as a source: `https://github.com/LibreBlogOrg/LibreblogRSS`

Or download the APK from the [Releases](https://github.com/LibreBlogOrg/LibreblogRSS/releases) page.
