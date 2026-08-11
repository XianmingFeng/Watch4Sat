---
layout: default
title: Watch4Sat Privacy Policy
---

# Watch4Sat Privacy Policy

Effective date: 2026-07-31

This policy describes the English-only Watch4Sat 1.0.0 release-candidate
application for Wear OS, package `com.xianming.watch4sat`.

Watch4Sat is a standalone satellite pass tracker. It has no user accounts, ads,
analytics, marketing SDK, or developer-operated backend. The app does not sell
personal data.

## Location And QTH

Watch4Sat needs a station location (QTH) to calculate passes, pointing
directions, distance, Doppler shift, ground tracks, and alerts. You may enter a
location manually, choose a point on a map, or explicitly request the watch's
location.

When you request GPS location, Watch4Sat prefers the Google Play services
Fused Location Provider and falls back to Android framework location providers
when fused location is unavailable or does not return an acceptable fix. The
app receives latitude, longitude, altitude, time, provider, and accuracy
information. It does not request background location. GPS acquisition is a
visible, cancellable action.

The chosen station latitude, longitude, altitude, time, source, optional
accuracy, and six-character grid locator are stored locally on the watch.
Pass snapshots derived from the station also remain local.

Watch4Sat does not send the saved station coordinates to a Watch4Sat server.
Google Play services and the operating system may process location as part of
providing a requested GPS or network fix under their own terms and privacy
policies.

## Online Services

Watch4Sat makes direct HTTPS requests from the watch to:

- CelesTrak, for amateur-satellite orbital elements;
- SatNOGS DB, for active transmitter metadata; and
- OpenStreetMap standard raster tile servers, when an online map is selected.

CelesTrak and SatNOGS requests do not include the saved QTH as a request
parameter. Like ordinary internet services, each provider can receive the
watch's network address, request time, requested URL, and Watch4Sat
User-Agent.

OpenStreetMap tile URLs contain zoom and tile coordinates. Those coordinates
reveal the approximate area currently viewed on the map and can therefore
reveal the approximate area of a saved or viewed QTH when the map is centered
there. OpenStreetMap also receives the network address, request time, and
Watch4Sat User-Agent. Watch4Sat does not bulk-download tiles for offline use.
Normal map tiles can be cached locally by the map library.

The embedded Natural Earth world map is processed entirely on the watch and
does not make a map-network request.

Provider information:

- CelesTrak: https://celestrak.org/
- SatNOGS DB: https://db.satnogs.org/
- OpenStreetMap: https://www.openstreetmap.org/copyright
- Google privacy policy: https://policies.google.com/privacy

## Data Stored On The Watch

Watch4Sat stores:

- the saved station location and app settings in Android DataStore;
- selected satellites, cached orbital elements, transmitter metadata, and
  refresh metadata in a Room database;
- calculated pass snapshots and pass-alert state in Android DataStore;
- ordinary OpenStreetMap tile cache files; and
- notification and alarm configuration required for enabled pass alerts.

Satellite names, pass times, and alert details can appear in watch
notifications and system surfaces when notifications are enabled.

Watch4Sat does not create developer-accessible cloud backups. Android backup
and device-to-device transfer are disabled for all application data.

## Diagnostics

Watch4Sat does not include remote crash reporting or analytics. Location
diagnostics can be written to the local Android system log and include provider
names, timing, fix age, accuracy, and result state. They intentionally exclude
latitude and longitude. Android and the device manufacturer may separately
collect operating-system diagnostics under their own policies.

## Deletion And Retention

Choosing the in-app action to clear the saved location removes the saved
station fields, coordinate-derived pass snapshots, pass-alert state, scheduled
pass alarms, and derived in-memory station/pass state. It does not remove the
downloaded public satellite/transmitter catalog, other preferences, or ordinary
map tile cache.

To delete all Watch4Sat data, use the Wear OS application settings to clear
Watch4Sat storage or uninstall Watch4Sat. Because Watch4Sat has no account or
developer backend, there is no server-side account record to delete.

Cached public orbital and transmitter records remain until refreshed, app
storage is cleared, or the app is uninstalled. Normal map tiles remain subject
to the map library cache lifecycle until app storage is cleared or the app is
uninstalled.

## Permissions

- Precise or approximate location: used only after an explicit location action.
- Notifications: used for pass alerts and applicable ongoing activity.
- Exact alarms: used to deliver enabled pass alerts at the requested time.
- Network access and network state: used for data refresh and online maps.
- Boot completion: used to restore applicable local alert scheduling.

## Children

Watch4Sat is a technical satellite-tracking utility and is not directed to
children. It does not knowingly collect personal information into a
developer-operated service.

## Changes And Contact

Material policy changes will update the effective date and the policy shipped
with the application.

Release contact: ba7lvg@foxmail.com

A stable public HTTPS copy of this policy is available at:
https://xianmingfeng.github.io/Watch4Sat/privacy/. This page, the repository
copy, and the policy packaged in the application are the maintained sources.
