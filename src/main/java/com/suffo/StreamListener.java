package com.suffo;

import com.github.twitch4j.helix.domain.Stream;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.*;

import java.util.*;
import java.util.concurrent.*;

public class StreamListener{

	private static final HashMap<String /*discord user id*/, String /*streamer id*/> streamerCache = new HashMap<>();
	private static final HashMap<String /*streamer id*/, Alert> alertCache = new HashMap<>();

	//offline alert edits will be stored here for two minutes, allowing a stream to go down for a short time before it's considered offline
	private static final HashMap<String /*discord user id*/, ScheduledFuture<?>> pendingOfflines = new HashMap<>();

	public static void onStreamStart(Member member){
		System.out.println("Stream started for " + member.getEffectiveName());
		Guild g = member.getGuild();
		Role streamerRole = g.getRoleById(Main.streamingRoleId);
		if(streamerRole == null){
			Logs.error("Failed to find streaming role (invalid role ID?)");
			return;
		}
		System.out.println("Adding role");
		Activity streamingActivity = member.getActivities().stream().filter(activity->activity.getType() == Activity.ActivityType.STREAMING && activity.isRich()).findFirst().orElse(null);
		RichPresence rp = streamingActivity == null ? null : streamingActivity.asRichPresence();
		if(rp == null){
			Logs.error("Failed to find streaming activity");
			return;
		}
		g.addRoleToMember(member, streamerRole).queue(succ->Logs.info("Set streaming role for " + member.getUser().getAsTag()),
													  err->Logs.error("Couldn't set streaming role for " + member.getUser().getAsTag(), err));

		MessageChannel channel = g.getChannelById(MessageChannel.class, Main.alertChannelId);
		if(channel == null){
			Logs.error("Couldn't get alert channel for " + member.getUser().getAsTag());
			return;
		}
		Stream stream = TwitchUtils.getCurrentStream(getStreamerId(member));
		if(stream == null){
			Logs.error("Couldn't get stream for " + member.getUser().getAsTag());
			return;
		}

		pendingOfflines.remove(member.getId());

		if(alertCache.containsKey(stream.getUserId())){
			channel.retrieveMessageById(alertCache.get(stream.getUserId()).messageId).queue(message->{
				message.editMessage(member.getAsMention() + " is now streaming " + MarkdownUtil.bold(MarkdownUtil.underline(MarkdownSanitizer.sanitize(stream.getTitle()))) + "\n" + rp.getUrl()).queue();
				alertCache.put(stream.getUserId(), new Alert(stream.getUserId(), stream.getId(), message.getId(), stream.getTitle()));
			}, err->{
				Logs.error("Couldn't edit alert message for " + member.getUser().getAsTag(), err);
			});
		}else{
			channel.sendMessage(member.getAsMention() + " is now streaming " + MarkdownUtil.bold(MarkdownUtil.underline(MarkdownSanitizer.sanitize(stream.getTitle()))) + "\n" + rp.getUrl())
					.setAllowedMentions(Collections.emptyList()) //disable pings in this message
				   .queue(msg->{
							  alertCache.put(member.getId(), new Alert(stream.getUserId(), stream.getId(), msg.getId(), stream.getTitle()));
						  },
						  err->Logs.error("Couldn't send streaming alert for " + member.getUser().getAsTag(), err));
		}
	}

	public static void onStreamEnd(Member member){
		Guild g = member.getGuild();
		Role streamerRole = g.getRoleById(Main.streamingRoleId);
		streamerCache.remove(member.getId());
		if(streamerRole == null) return; //shouldn't be null as checked before this method but the warning is annoying
		g.removeRoleFromMember(member, streamerRole).queue(succ->Logs.info("Removed streaming role for " + member.getUser().getAsTag()),
														   err->Logs.error("Couldn't remove streaming role for " + member.getUser().getAsTag(), err));
		Alert alert = alertCache.get(member.getId());
		if(alert == null) return;
		MessageChannel channel = g.getChannelById(MessageChannel.class, Main.alertChannelId);
		if(channel == null){
			Logs.error("Couldn't get alert channel for " + member.getUser().getAsTag());
			return;
		}
		pendingOfflines.put(
				member.getId(),
				channel.retrieveMessageById(alert.messageId)
					   .queueAfter(2, TimeUnit.MINUTES,
								   msg->{
									   String vod = TwitchUtils.getVodId(alert.streamerId, alert.streamId);
									   if(vod != null) vod = "https://www.twitch.tv/videos/" + vod;
									   String vodLink = vod == null ? "" : "\n" + MarkdownUtil.bold("VOD: ") + vod;
									   msg.editMessage(member.getAsMention() + " was streaming " + MarkdownUtil.bold(MarkdownUtil.underline(MarkdownSanitizer.sanitize(alert.streamTitle))) + vodLink)
										  .queue(succ->{
													 alertCache.remove(member.getId());
													 pendingOfflines.remove(member.getId());
												 },
												 err->{
													 Logs.error("Couldn't edit streaming alert for " + member.getUser().getAsTag(), err);
													 alertCache.remove(member.getId());
												 });
								   },
								   err->{
									   Logs.error("Couldn't edit streaming alert message for " + member.getUser().getAsTag(), err);
									   alertCache.remove(member.getId());
								   }

					   )
		);
	}

	private static String getStreamerUsernameFromRichPresence(Member member){
		Activity activity = member.getActivities().stream().filter(a->a.getType() == Activity.ActivityType.STREAMING).findFirst().orElse(null);
		if(activity == null || activity.getUrl() == null) return null;
		return activity.getUrl().contains("twitch.tv") ? activity.getUrl().substring(activity.getUrl().lastIndexOf("/") + 1) : null;
	}

	private static String getStreamerId(Member member){
		String id = streamerCache.get(member.getId());
		if(id == null) id = TwitchUtils.getUserId(getStreamerUsernameFromRichPresence(member));
		if(id != null) streamerCache.put(member.getId(), id);
		return id;
	}

	public static void checkStillStreaming(){
		Guild g = Main.jda.getGuildById(Main.guildId);
		if(g == null) return;
		Role streamerRole = g.getRoleById(Main.streamingRoleId);
		if(streamerRole == null) return;
		g.loadMembers(member->{
			if(!member.getRoles().contains(streamerRole) && Main.isMemberStreamingGTAV(member)) onStreamStart(member);
			else if(member.getRoles().contains(streamerRole) && !Main.isMemberStreamingGTAV(member))
				onStreamEnd(member);
		});
	}

	public static class Alert{
		public final String streamerId;
		public final String streamId;
		public final String messageId;
		public final String streamTitle;

		public Alert(String streamerId, String streamId, String messageId, String streamTitle){
			this.streamerId = streamerId;
			this.streamId = streamId;
			this.messageId = messageId;
			this.streamTitle = streamTitle;
		}
	}

}
