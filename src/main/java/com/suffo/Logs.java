package com.suffo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.*;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.spi.FilterReply;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.SplitUtil;
import org.fusesource.jansi.Ansi;
import org.slf4j.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Logs extends ConsoleAppender<ILoggingEvent>{

	private static final ByteArrayOutputStream discordLogStream = new ByteArrayOutputStream();
	public static final Charset charset = StandardCharsets.UTF_8;

	public static Logger getLogger(){
		StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
		StackWalker.StackFrame frame = walker.walk(stack->{
			for(StackWalker.StackFrame element : stack.toList()){
				if(!element.getClassName().equals(Logs.class.getName())){
					return element;
				}
			}
			return null;
		});
		if(frame != null){
			return LoggerFactory.getLogger(frame.getDeclaringClass());
		}
		return LoggerFactory.getLogger(walker.getCallerClass());
	}

	public static void evalLog(Object o){
		System.out.println(o);
	}

	public static void debug(Object x, boolean... toDiscord){
		if(x instanceof Throwable){
			debug((Throwable) x, toDiscord);
			return;
		}
		if(toDiscord == null || toDiscord.length == 0 || toDiscord[0]){
			getLogger().debug(x.toString());
		}else{
			System.out.print(getTimestamp() + " ");
			System.out.println(x);
		}
	}

	public static void debug(Throwable x, boolean... toDiscord){
		if(toDiscord == null || toDiscord.length == 0 || toDiscord[0]){
			getLogger().debug("I have to put a string here, I don't know how it looks", x);
		}else{
			System.out.print(getTimestamp() + " ");
			System.out.print(Ansi.ansi().fgBrightCyan());
			x.printStackTrace();
			System.out.print(Ansi.ansi().reset());
		}
	}

	public static void debug(String message, Throwable x, boolean... toDiscord){
		if(toDiscord == null || toDiscord.length == 0 || toDiscord[0]){
			getLogger().debug(message, x);
		}else{
			System.out.print(getTimestamp() + " ");
			System.out.print(Ansi.ansi().fgBrightCyan());
			System.out.println(message);
			x.printStackTrace();
			System.out.print(Ansi.ansi().reset());
		}
	}

	public static void info(Object x, boolean... toDiscord){
		if(x instanceof Throwable){
			info((Throwable) x, toDiscord);
			return;
		}

		if(toDiscord == null || toDiscord.length == 0 || toDiscord[0]){
			getLogger().info(x.toString());
		}else{
			System.out.print(getTimestamp() + " ");
			System.out.println(x);
		}
	}

	public static void info(Throwable x, boolean... toDiscord){
		if(toDiscord == null || toDiscord.length == 0 || toDiscord[0]){
			getLogger().info("I have to put a string here, I don't know how it looks", x);
		}else{
			System.out.print(getTimestamp() + " ");
			System.out.print(Ansi.ansi().fgBrightBlue());
			x.printStackTrace();
			System.out.print(Ansi.ansi().reset());
		}
	}

	public static void info(String message, Throwable x, boolean... toDiscord){
		if(toDiscord == null || toDiscord.length == 0 || toDiscord[0]){
			getLogger().info(message, x);
		}else{
			System.out.print(getTimestamp() + " ");
			System.out.print(Ansi.ansi().fgBrightBlue());
			System.out.println(message);
			x.printStackTrace();
			System.out.print(Ansi.ansi().reset());
		}
	}

	public static void warn(Object x, boolean... toDiscord){
		if(x instanceof Throwable){
			warn((Throwable) x, toDiscord);
			return;
		}
		if(toDiscord == null || toDiscord.length == 0 || toDiscord[0]){
			getLogger().warn(x.toString());
		}else{
			System.out.print(getTimestamp() + " ");
			System.out.println(x);
		}
	}

	public static void warn(Throwable x, boolean... toDiscord){
		if(toDiscord == null || toDiscord.length == 0 || toDiscord[0]){
			getLogger().warn("I have to put a string here, I don't know how it looks", x);
		}else{
			System.out.print(getTimestamp() + " ");
			System.out.print(Ansi.ansi().fgBrightYellow());
			x.printStackTrace();
			System.out.print(Ansi.ansi().reset());
		}
	}

	public static void warn(String message, Throwable x, boolean... toDiscord){
		if(toDiscord == null || toDiscord.length == 0 || toDiscord[0]){
			getLogger().warn(message, x);
		}else{
			System.out.print(getTimestamp() + " ");
			System.out.print(Ansi.ansi().fgBrightYellow());
			System.out.println(message);
			x.printStackTrace();
			System.out.print(Ansi.ansi().reset());
		}
	}

	public static void error(Object x, boolean... toDiscord){
		if(x instanceof Throwable){
			error((Throwable) x, toDiscord);
			return;
		}
		if(toDiscord == null || toDiscord.length == 0 || toDiscord[0]){
			getLogger().error(x.toString());
		}else{
			System.out.print(getTimestamp() + " ");
			System.out.println(x);
		}
	}

	public static void error(Throwable x, boolean... toDiscord){
		if(toDiscord == null || toDiscord.length == 0 || toDiscord[0]){
			getLogger().error("I have to put a string here, I don't know how it looks", x);
		}else{
			System.out.print(getTimestamp() + " ");
			System.out.print(Ansi.ansi().fgBrightRed());
			x.printStackTrace();
			System.out.print(Ansi.ansi().reset());
		}
	}

	public static void error(String message, Throwable x, boolean... toDiscord){
		if(toDiscord == null || toDiscord.length == 0 || toDiscord[0]){
			getLogger().error(message, x);
		}else{
			System.out.print(getTimestamp() + " ");
			System.out.print(Ansi.ansi().fgBrightRed());
			System.out.println(message);
			x.printStackTrace();
			System.out.print(Ansi.ansi().reset());
		}
	}

	public static void log(){
		System.out.println();
	}

	private static final int logSize = 1980;


	private static WebhookClient client;
	public static String webhookUrl = Main.properties.getProperty("loggingWebhookUrl");

	public static void logToDiscord(String text, String syntax){
		if(Main.getJDA() == null || webhookUrl == null) return;

		try{
			if(client == null || client.isShutdown())
				try{
					client = WebhookClient.withUrl(webhookUrl);
				}catch(Exception e){
					if(e instanceof IllegalArgumentException){
						// Invalid URL
						Logs.error("Invalid webhook URL: " + webhookUrl);
						webhookUrl = null;
						return;
					}
				}
			for(String sublog : SplitUtil.split(convertAnsiColours(text), logSize, SplitUtil.Strategy.NEWLINE)){
				client.send(new WebhookMessageBuilder().setContent("```" + syntax + "\n" + sublog + "```")
													   .setUsername(Main.getJDA().getSelfUser().getName() + " Logging")
													   .setAvatarUrl(Main.getJDA().getSelfUser().getEffectiveAvatarUrl())
													   .build());
			}
		}catch(Exception e){
			Logs.error(e, false);
		}

	}

	public static void logToDiscord(String text){
		logToDiscord(text, "ansi");
	}

	public static void logExceptionsToDiscord(String text){
		logToDiscord(text/*, "nim"*/);
	}

	//discord's ansi colours don't quite match up with the ones in the console
	public static String convertAnsiColours(String text){
		return text
				.replace(Ansi.ansi().fgBlack().toString(), ANSICodes.ANSI_BLACK)
				.replace(Ansi.ansi().bgDefault().toString(), ANSICodes.ANSI_BLACK_BACKGROUND)
				.replace(Ansi.ansi().fgBrightRed().toString(), ANSICodes.ANSI_RED)
				.replace(Ansi.ansi().bgBrightRed().toString(), ANSICodes.ANSI_RED_BACKGROUND)
				.replace(Ansi.ansi().fgBrightGreen().toString(), ANSICodes.ANSI_GREEN)
				.replace(Ansi.ansi().bgBrightGreen().toString(), ANSICodes.ANSI_GREEN_BACKGROUND)
				.replace(Ansi.ansi().fgBrightYellow().toString(), ANSICodes.ANSI_YELLOW)
				.replace(Ansi.ansi().bgBrightYellow().toString(), ANSICodes.ANSI_YELLOW_BACKGROUND)
				.replace(Ansi.ansi().fgBrightBlue().toString(), ANSICodes.ANSI_BLUE)
				//.replace(Ansi.ansi().bg().toString(), ANSICodes.ANSI_BLUE_BACKGROUND) no blue bg
				.replace(Ansi.ansi().fgBrightMagenta().toString(), ANSICodes.ANSI_PURPLE)
				.replace(Ansi.ansi().bgBrightMagenta().toString(), ANSICodes.ANSI_PURPLE_BACKGROUND)
				.replace(Ansi.ansi().fgBrightCyan().toString(), ANSICodes.ANSI_CYAN)
				.replace(Ansi.ansi().bgBrightCyan().toString(), ANSICodes.ANSI_CYAN_BACKGROUND)
				.replace(Ansi.ansi().fgDefault().toString(), ANSICodes.ANSI_WHITE)
				.replace(Ansi.ansi().reset().toString(), ANSICodes.ANSI_RESET)
				//.replace(Ansi.ansi().bg.toString(), ANSICodes.ANSI_WHITE_BACKGROUND) no white bg
				;
	}

	//start a timer to log the contents of the log stream to discord every 5 seconds
	static{
		Main.scheduler.scheduleAtFixedRate(()->{
			if(Main.getJDA() != null && Main.getJDA().getStatus() == JDA.Status.CONNECTED){
					String log = discordLogStream.toString(charset);
					if(log != null && !log.isBlank()) logToDiscord(log);
				}
				discordLogStream.reset();
		}, 0, 5000, TimeUnit.MILLISECONDS);
	}

	public static String getTimestamp(){
		Date d = new Date(System.currentTimeMillis());
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTime(d);
		return String.format("[%02d/%02d - %02d:%02d:%02d]", calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
	}

	@Override
	public void doAppend(ILoggingEvent event){
		if(getFilterChainDecision(event) == FilterReply.DENY){
			return;
		}
		byte[] encoded = encoder.encode(event);
		String log = new String(encoded, charset);
		if(log.contains("WebhookClient") && event.getLevel() == Level.ERROR) return; //don't log webhook errors as they'll spam the logs in an infinite loop
		try(PrintStream ps = new PrintStream(discordLogStream, true, charset)){
			ps.print(log);
		}
	}


	@SuppressWarnings("unused")
	public static class ANSICodes{

		public static final String ANSI_ESCAPE = "\u001B[";
		public static final String ANSI_RESET = ANSI_ESCAPE+"0m";
		public static final String ANSI_BLACK = ANSI_ESCAPE+"30m";
		public static final String ANSI_RED = ANSI_ESCAPE+"31m";
		public static final String ANSI_GREEN = ANSI_ESCAPE+"32m";
		public static final String ANSI_YELLOW = ANSI_ESCAPE+"33m";
		public static final String ANSI_BLUE = ANSI_ESCAPE+"34m";
		public static final String ANSI_PURPLE = ANSI_ESCAPE+"35m";
		public static final String ANSI_CYAN = ANSI_ESCAPE+"36m";
		public static final String ANSI_WHITE = ANSI_ESCAPE+"37m";
		public static final String ANSI_BLACK_BACKGROUND = ANSI_ESCAPE+"40m";
		public static final String ANSI_RED_BACKGROUND = ANSI_ESCAPE+"41m";
		public static final String ANSI_GREEN_BACKGROUND = ANSI_ESCAPE+"42m";
		public static final String ANSI_YELLOW_BACKGROUND = ANSI_ESCAPE+"43m";
		public static final String ANSI_BLUE_BACKGROUND = ANSI_ESCAPE+"44m";
		public static final String ANSI_PURPLE_BACKGROUND = ANSI_ESCAPE+"45m";
		public static final String ANSI_CYAN_BACKGROUND = ANSI_ESCAPE+"46m";
		public static final String ANSI_WHITE_BACKGROUND = ANSI_ESCAPE+"47m";

		public static final String LOGBACK_RESET = ANSI_ESCAPE+"0;39m";
		public static final String[] ANSI_COLORS = {LOGBACK_RESET, ANSI_RESET, ANSI_BLACK, ANSI_RED, ANSI_GREEN, ANSI_YELLOW, ANSI_BLUE, ANSI_PURPLE, ANSI_CYAN, ANSI_WHITE, ANSI_BLACK_BACKGROUND, ANSI_RED_BACKGROUND, ANSI_GREEN_BACKGROUND, ANSI_YELLOW_BACKGROUND, ANSI_BLUE_BACKGROUND, ANSI_PURPLE_BACKGROUND, ANSI_CYAN_BACKGROUND, ANSI_WHITE_BACKGROUND};
	}
}
