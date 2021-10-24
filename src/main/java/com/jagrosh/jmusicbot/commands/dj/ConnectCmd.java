package com.jagrosh.jmusicbot.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.commands.DJCommand;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


public class ConnectCmd extends DJCommand {
    public ConnectCmd(Bot bot) {
        super(bot);
        this.name = "connect";
        this.help = "bot will leave and connect specified channel";
        this.arguments = "<nothing or channel id>";
        this.aliases = bot.getConfig().getAliases(this.name);
    }

    public void doCommand(CommandEvent event)  {
        Logger log = LoggerFactory.getLogger("MusicBot");
        AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
        VoiceChannel current = event.getGuild().getSelfMember().getVoiceState().getChannel();
        VoiceChannel userState = event.getMember().getVoiceState().getChannel();
        String args = event.getArgs();

        if(userState != null) {
            if(args != null && !args.isEmpty()) {
                if(event.getGuild().getVoiceChannelById(args) != null) {
                    if(current != null) {
                        try {
                            if(current.getIdLong() != Long.valueOf(args)) {
                                try {
                                    if (handler.getPlayingTrack() != null) handler.getPlayer().setPaused(true);
                                    event.getGuild().getAudioManager().openAudioConnection(event.getGuild().getVoiceChannelById(event.getArgs()));
                                } catch (Exception ignored) {
                                } finally {
                                    if (handler.getPlayingTrack() != null) handler.getPlayer().setPaused(false);
                                    event.replySuccess("Now in voice: <#" + event.getArgs() + ">");
                                }
                            } else {
                                event.replyError("Im already connected to this channel!");
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            if (handler.getPlayingTrack() != null) handler.getPlayer().setPaused(true);
                            event.getGuild().getAudioManager().openAudioConnection(event.getGuild().getVoiceChannelById(event.getArgs()));
                        } catch (Exception ignored) {
                        } finally {
                            if (handler.getPlayingTrack() != null) handler.getPlayer().setPaused(false);
                            event.replySuccess("Now in voice: <#" + event.getArgs() + ">");
                        }
                    }
                } else {
                    event.replyError("Invalid channel!");
                }
            } else {
                try {
                    if(handler.getPlayingTrack() != null) handler.getPlayer().setPaused(true);
                    event.getGuild().getAudioManager().openAudioConnection(userState);
                } catch (Exception ignored) {}
                finally {
                    if(handler.getPlayingTrack() != null) handler.getPlayer().setPaused(false);
                    event.replySuccess("Now in voice: <#" + userState.getIdLong() + ">");
                }
            }
        } else if(args.isEmpty()) {
            event.replyError("Empty channel!");
        } else {
            if(event.getGuild().getVoiceChannelById(args) != null) {
                if(current != null) {
                    try {
                        if(current.getIdLong() != Long.valueOf(args)) {
                            try {
                                if (handler.getPlayingTrack() != null) handler.getPlayer().setPaused(true);
                                event.getGuild().getAudioManager().openAudioConnection(event.getGuild().getVoiceChannelById(event.getArgs()));
                            } catch (Exception ignored) {
                            } finally {
                                if (handler.getPlayingTrack() != null) handler.getPlayer().setPaused(false);
                                event.replySuccess("Now in voice: <#" + event.getArgs() + ">");
                            }
                        } else {
                            event.replyError("Im already connected to this channel!");
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        if (handler.getPlayingTrack() != null) handler.getPlayer().setPaused(true);
                        event.getGuild().getAudioManager().openAudioConnection(event.getGuild().getVoiceChannelById(event.getArgs()));
                    } catch (Exception ignored) {
                    } finally {
                        if (handler.getPlayingTrack() != null) handler.getPlayer().setPaused(false);
                        event.replySuccess("Now in voice: <#" + event.getArgs() + ">");
                    }
                }
            } else {
                event.replyError("Invalid channel!");
            }
        }
    }
}


