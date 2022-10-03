package com.suffo;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.user.update.UserUpdateActivitiesEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.*;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Main extends ListenerAdapter{

	public static JDA jda;
	public static Properties properties;
	public static String token;
	public static String guildId;
	public static String alertChannelId;
	public static String streamingRoleId;

	public static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

	static{
		properties = new Properties();
		File file = new File("config.properties");
		if(!file.exists()){
			try{
				if(file.createNewFile()){
					Scanner scanner = new Scanner(System.in);
					System.out.println("First time setup required. This will be saved in the \"config.properties\" file in the working directory. Any changes to this file will require a restart of the bot.");
					System.out.println("Enter token:");
					properties.setProperty("token", scanner.nextLine());
					System.out.println("Enter server ID:");
					properties.setProperty("guildId", scanner.nextLine());
					System.out.println("Enter alert channel ID:");
					properties.setProperty("alertChannelId", scanner.nextLine());
					System.out.println("Enter streaming role ID:");
					properties.setProperty("streamingRoleId", scanner.nextLine());
					System.out.println("Enter logging webhook url (optional):");
					properties.setProperty("loggingWebhookUrl", scanner.nextLine());
					System.out.println("Enter Twitch client ID (from Twitch developer console):");
					properties.setProperty("twitchClientId", scanner.nextLine());
					System.out.println("Enter Twitch client secret (from Twitch developer console):");
					properties.setProperty("twitchClientSecret", scanner.nextLine());
					properties.store(new FileOutputStream(file), null);
				}else{
					System.out.println("Failed to create config.properties file.");
					System.exit(1);
				}
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		try{
			properties.load(new FileInputStream("config.properties"));
			token = properties.getProperty("token");
			guildId = properties.getProperty("guildId");
			alertChannelId = properties.getProperty("alertChannelId");
			streamingRoleId = properties.getProperty("streamingRoleId");
			Logs.webhookUrl = properties.getProperty("loggingWebhookUrl");
			TwitchUtils.TWITCH_CLIENT_ID = properties.getProperty("twitchClientId");
			TwitchUtils.TWITCH_CLIENT_SECRET = properties.getProperty("twitchClientSecret");
		}catch(IOException ignored){}
	}

	public static void main(String[] args){
		try{
			jda = login();
		}catch(Exception e){
			Logs.error("Failed to login", e);
			Scanner scanner = new Scanner(System.in);
			System.out.println("Please ensure all values in config.properties are correct. Press enter to exit.");
			scanner.nextLine();
			System.exit(1);
		}

		Guild g = jda.getGuildById(guildId);
		if(g == null){
			Logs.error("Failed to find guild with id: " + guildId);
			return;
		}
		Role streamingRole = g.getRoleById(streamingRoleId);
		if(streamingRole == null){
			Logs.error("Failed to find role with id: " + streamingRoleId);
		}
		MessageChannel alertChannel = g.getChannelById(MessageChannel.class, alertChannelId);
		if(alertChannel == null){
			Logs.error("Failed to find channel with id: " + alertChannelId);
		}

		//every two minutes, scan the entire member list for streaming members or members with the streaming role and check if they should have the role added or removed
		scheduler.scheduleAtFixedRate(()->{
			try{
				StreamListener.checkStillStreaming();
			}catch(Exception e){
				Logs.error(e);
			}
		}, 0, 2, TimeUnit.MINUTES);
	}

	public static JDA getJDA(){
		return jda;
	}

	private static JDA login() throws InterruptedException{
		return JDABuilder
				.createDefault(token)
				//only requires the ability to see members and their activities
				.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.enableCache(CacheFlag.ACTIVITY)
				.addEventListeners(new Main())
				.build()
				.awaitReady();
	}

	@Override
	public void onUserUpdateActivities(UserUpdateActivitiesEvent event){
		if(!event.getGuild().getId().equals(guildId)) return;

		Role streamerRole = event.getGuild().getRoleById(streamingRoleId);
		if(streamerRole == null){
			Logs.error("Role with ID " + streamingRoleId + " not found, please add the correct role ID to the config file and restart");
			jda.shutdownNow();
			return;
		}
		//if member has a streaming activity and is streaming GTA V but doesn't have streamer role, pass along to StreamListener where it will be validated
		if(isMemberStreamingGTAV(event.getMember()) && !event.getMember().getRoles().contains(event.getGuild().getRoleById(streamingRoleId))){
			StreamListener.onStreamStart(event.getMember());
		}
		//if member has a streaming activity but is not streaming GTA V and has streamer role, pass to StreamListener to remove role and edit alert
		else if(event.getMember().getRoles().stream().anyMatch(r->r.getId().equals(streamingRoleId))){
			StreamListener.onStreamEnd(event.getMember());
		}

	}

	//check if the Discord activity is a GTA V stream (no twitch API communication involved here)
	public static boolean isMemberStreamingGTAV(Member member){
		if(member.getActivities().isEmpty() || member.getActivities().stream().noneMatch(a -> a.getType() == Activity.ActivityType.STREAMING)) return false;
		String game = member.getActivities().stream().filter(activity->activity.getType() == Activity.ActivityType.STREAMING && activity.isRich()).findFirst().map(activity->activity.asRichPresence().getState()).orElse(null);
		return "Grand Theft Auto V".equalsIgnoreCase(game) || member.getUser().getName().equalsIgnoreCase("suffocate");
	}
}
