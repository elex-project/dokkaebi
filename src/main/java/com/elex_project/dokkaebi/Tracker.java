package com.elex_project.dokkaebi;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * TRACKER
 * for Google Analytics Measurement Protocol
 *
 * @see "https://developers.google.com/analytics/devguides/collection/protocol/v1"
 * @see "https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters"
 */
@Slf4j
public final class Tracker {
	/*
	 * The Protocol version. The current value is '1'.
	 * This will only change when there are changes made that are not backwards compatible.
	 */
	private static final String VERSION = "1";
	private static final String END_POINT = "https://www.google-analytics.com/collect";
	private static final String USER_AGENT = "Dokkaebi/1.0";
	private static final String ACCEPT_LANG = "ko-kr,ko;q=0.8,en-us;q=0.5,en;q=0.3";

	private static Tracker instance;

	/**
	 *
	 * @param trackingId UA-xxxxxxxx-x
	 * @param clientId keep this somewhere, and reuse for a same user.
	 * @return tracker
	 */
	public static Tracker getInstance(final String trackingId, final UUID clientId) {
		if (null == instance) {
			instance = new Tracker(trackingId, clientId);
		}
		return instance;
	}

	private final HttpClient httpClient;

	private Tracker(final @NotNull String trackingId, final @NotNull UUID clientId) {
		this.trackingId = trackingId;
		this.clientId = clientId;

		this.httpClient = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_2)
				.executor(Executors.newCachedThreadPool())
				.build();
	}

	/*
	This field is required if User ID (uid) is not specified in the request.
	This anonymously identifies a particular user, device, or browser instance.
	 */
	private final UUID clientId;
	/*
	 * The tracking ID / web property ID. The format is UA-XXXX-Y. All collected data is associated by this ID.
	 */
	private final String trackingId;

	private String appName, appVersion, appId, appInstallerId;
	private String screenResolution, viewportSize;

	public void setScreenResolution(final int w, final int h) {
		this.screenResolution = w + "x" + h;
	}

	public void setViewportSize(final int w, final int h) {
		this.viewportSize = w + "x" + h;
	}

	public void setAppName(final String appName) {
		this.appName = appName;
	}

	public void setAppVersion(final String appVersion) {
		this.appVersion = appVersion;
	}

	public void setAppId(final String appId) {
		this.appId = appId;
	}

	public void setAppInstallerId(final String appInstallerId) {
		this.appInstallerId = appInstallerId;
	}

	private void transport(final Map<String, String> payload) {

		httpClient.sendAsync(HttpRequest.newBuilder()
						.uri(URI.create(END_POINT))
						.header("User-Agent", USER_AGENT)
						.header("Accept-Language", ACCEPT_LANG)
						.POST(HttpRequest.BodyPublishers
								.ofString(serialize(payload)))
						.build(),
				HttpResponse.BodyHandlers.ofString())
				.thenAcceptAsync(resp ->
						log.debug("Response {}", resp.statusCode()));
	}

	private Map<String, String> payloadBuilder() {
		Map<String, String> payload = new HashMap<>();
		payload.put("v", VERSION);//! Protocol Version
		payload.put("tid", trackingId);//! Tracking ID
		payload.put("cid", clientId.toString());//! Client ID

		payload.put("ds", "app");
		//payload.put("geoid", Locale.getDefault().getCountry());
		return payload;
	}

	/**
	 * 어플리케이션 시작
	 *
	 * @param appName    이름
	 * @param appVersion 버전
	 * @param appId      패키지네임
	 */
	public void appStart(final @NotNull String appName, final @NotNull String appVersion, final @NotNull String appId) {
		this.appName = appName;
		this.appVersion = appVersion;
		this.appId = appId;

		Map<String, String> payload = payloadBuilder();

		payload.put("an", appName); //! Application Name
		payload.put("av", appVersion);//Application Version
		payload.put("aid", appId);//Application ID
		if (null != appInstallerId) payload.put("aiid", appInstallerId);//! Screen Name

		payload.put("t", "event");//! hit type
		payload.put("ec", "Application");//Event Category
		payload.put("ea", "App Start");//Event Action
		payload.put("sc", "start");//session start

		if (null != screenResolution) payload.put("sr", screenResolution);// Screen Resolution
		if (null != viewportSize) payload.put("vp", viewportSize);

		payload.put("ul", Locale.getDefault().toString().replace("_", "-").toLowerCase());//user language
		payload.put("cd1", System.getenv("os.name"));//custom dimension
		payload.put("cd2", System.getenv("os.version"));
		payload.put("cd3", System.getenv("java.runtime.name"));
		payload.put("cd4", System.getenv("java.version"));
		payload.put("cm1", String.valueOf(Runtime.version().feature()));

		transport(payload);
	}

	/**
	 * 어플리케이션 종료
	 */
	public void appEnd() {
		Map<String, String> payload = payloadBuilder();
		payload.put("t", "event");//! hit type
		payload.put("ec", "Application");//Event Category
		payload.put("ea", "App Exit");//Event Action
		payload.put("sc", "end");//session end

		transport(payload);
	}

	/**
	 * 화면 전환 추적
	 *
	 * @param screenName 화면 이름
	 * @see "https://developers.google.com/analytics/devguides/collection/protocol/v1/devguide#screenView"
	 */
	public void trackScreen(final @NotNull String screenName) {
		Map<String, String> payload = payloadBuilder();

		payload.put("t", "screenview");
		if (null != appName) payload.put("an", appName); //! Application Name
		if (null != appVersion) payload.put("av", appVersion);//Application Version
		if (null != appId) payload.put("aid", appId);//Application ID
		payload.put("cd", screenName);//! Screen Name

		transport(payload);
	}

	/**
	 * 이벤트 추적
	 *
	 * @param category Specifies the event category. Must not be empty.
	 * @param action   Specifies the event action. Must not be empty.
	 * @param label    Specifies the event label.
	 * @param value    Specifies the event value. Values must be non-negative.
	 * @see "https://developers.google.com/analytics/devguides/collection/protocol/v1/devguide#event"
	 */
	public void trackEvent(final @NotNull String category, final @NotNull String action, final String label, long value) {
		Map<String, String> payload = payloadBuilder();

		payload.put("t", "event");//! hit type
		payload.put("ec", category);//Event Category
		payload.put("ea", action);//Event Action
		if (null != label) payload.put("el", label);//Event Label
		payload.put("ev", String.valueOf(value));//Event Value

		transport(payload);
	}

	/**
	 * 이벤트 추적
	 *
	 * @param category Specifies the event category. Must not be empty.
	 * @param action   Specifies the event action. Must not be empty.
	 */
	public void trackEvent(final @NotNull String category, final @NotNull String action) {
		Map<String, String> payload = payloadBuilder();

		payload.put("t", "event");//! hit type
		payload.put("ec", category);//Event Category
		payload.put("ea", action);//Event Action

		transport(payload);
	}

	/**
	 * 타이밍 추적
	 *
	 * @param category Specifies the user timing category.
	 * @param variable Specifies the user timing variable.
	 * @param label    Specifies the user timing label.
	 * @param timing   Specifies the user timing value. The value is in milliseconds.
	 * @see "https://developers.google.com/analytics/devguides/collection/protocol/v1/devguide#usertiming"
	 */
	public void trackTiming(final @NotNull String category, final @NotNull String variable, final String label, final long timing) {
		Map<String, String> payload = payloadBuilder();

		payload.put("t", "timing");
		payload.put("utc", category);//UserInfo timing category
		payload.put("utv", variable);//UserInfo timing variable name
		payload.put("utt", String.valueOf(timing));//UserInfo timing time
		payload.put("utl", label);//UserInfo timing label

		transport(payload);
	}

	/**
	 * 예외 추적
	 *
	 * @param description Specifies the description of an exception. 150 Bytes MAX
	 * @param fatal       Specifies whether the exception was fatal.
	 * @see "https://developers.google.com/analytics/devguides/collection/protocol/v1/devguide#exception"
	 */
	public void trackException(final String description, final boolean fatal) {
		Map<String, String> payload = payloadBuilder();

		payload.put("t", "exception");//! hit type
		payload.put("an", appName);//! Application Name

		payload.put("exd", description);//Exception Description
		payload.put("exf", fatal ? "1" : "0");//Is Exception Fatal?

		transport(payload);
	}

	/**
	 * 예외 추적
	 *
	 * @param description Specifies the description of an exception. 150 Bytes MAX
	 */
	public void trackException(final String description) {
		Map<String, String> payload = payloadBuilder();

		payload.put("t", "exception");//! hit type
		payload.put("an", appName);//! Application Name

		payload.put("exd", description);//Exception Description
		//payload.put("exf", fatal?"1":"0");//Is Exception Fatal?

		transport(payload);
	}

	private static String serialize(final Map<String, String> map) {
		/*
		:: Cache Busting
		Used to send a random number in GET requests to ensure browsers and proxies don't cache hits.
		It should be sent as the final parameter of the request since we've seen some 3rd party internet filtering software
		add additional parameters to HTTP requests incorrectly.
		This value is not used in reporting.
		 */
		map.put("z", String.valueOf(System.currentTimeMillis()));
		//
		final StringJoiner joiner = new StringJoiner("&");
		for (final String key : map.keySet()) {
			joiner.add(key + "=" + URLEncoder.encode(map.get(key), StandardCharsets.UTF_8));
		}
		return joiner.toString();
	}
}
