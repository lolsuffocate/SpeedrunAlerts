package com.suffo;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.*;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.common.util.ThreadUtils;
import com.github.twitch4j.helix.domain.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class TwitchUtils{

	public static TwitchClient twitchClient;
	public static String TWITCH_CLIENT_ID = Main.properties.getProperty("twitchClientId");
	public static String TWITCH_CLIENT_SECRET = Main.properties.getProperty("twitchClientSecret");
	private static final TwitchIdentityProvider tip = new TwitchIdentityProvider(TWITCH_CLIENT_ID, TWITCH_CLIENT_SECRET, null);
	public static final AtomicReference<OAuth2Credential> credential = new AtomicReference<>(tip.getAppAccessToken());

	static{
		final ScheduledThreadPoolExecutor exec = ThreadUtils.getDefaultScheduledThreadPoolExecutor("exec", Runtime.getRuntime().availableProcessors() * 2);
		exec.scheduleAtFixedRate(
				()->credential.set(tip.getAppAccessToken()),
				1L,
				1L,
				TimeUnit.DAYS
		);
		twitchClient = TwitchClientBuilder.builder()
										  .withClientId(TWITCH_CLIENT_ID)
										  .withClientSecret(TWITCH_CLIENT_SECRET)
										  .withDefaultAuthToken(credential.get())
										  .withScheduledThreadPoolExecutor(exec)
										  .withEnableHelix(true)
										  .withWsPingPeriod(10)
										  .build();
	}

	public static String getUserId(String username){
		try{
			return twitchClient.getHelix().getUsers(null, null, Collections.singletonList(username)).execute().getUsers().get(0).getId();
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	public static Stream getCurrentStream(String streamerId){
		List<Stream> streams = twitchClient.getHelix().getStreams(null, null, null, null, null, null, Collections.singletonList(streamerId), null).execute().getStreams();
		return streams.isEmpty() ? null : streams.get(0);
	}

	private static final HashMap<String, String> vodIdCache = new HashMap<>();

	public static String getVodId(String channelId, String streamId){
		try{
			if(streamId == null){
				Logs.warn("Cannot get vod as stream ID is null");
				return null;
			}
			if(vodIdCache.containsKey(streamId)) return vodIdCache.get(streamId);
			else{
				String vodId = vodIdCache.containsKey(streamId) ? vodIdCache.get(streamId) : twitchClient.getHelix().getVideos(null, null, channelId, null, null, null, "time", null, null, null, 50).execute().getVideos().stream().filter(v->streamId.equals(v.getStreamId())).map(Video::getId).findFirst().orElse(null);
				vodIdCache.put(streamId, vodId);
				return vodId;
			}
		}catch(Exception e){
			Logs.warn("Failed to get VOD ID for channel: " + channelId + " and stream ID: " + streamId);
			return null;
		}
	}

}
