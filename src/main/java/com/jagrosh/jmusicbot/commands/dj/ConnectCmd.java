package com.jagrosh.jmusicbot.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.commands.DJCommand;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;


public class ConnectCmd extends DJCommand {
    public ConnectCmd(Bot bot) {
        super(bot);
        this.name = "connect";
        this.help = "bot will leave and connect specified channel";
        this.arguments = "<nothing or channel id>";
        this.aliases = bot.getConfig().getAliases(this.name);
    }

    public void doCommand(CommandEvent event)  {
        AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();

        if(event.getArgs().isEmpty()) {
            if(event.getMember().getVoiceState().inVoiceChannel()) {
                if(handler.getPlayingTrack() != null) {
                    handler.getPlayer().setPaused(true);

                    event.getGuild().getAudioManager().openAudioConnection(event.getMember().getVoiceState().getChannel());

                    handler.getPlayer().setPaused(false);

                    event.replySuccess("Now in voice: " +  event.getGuild().getSelfMember().getVoiceState().getChannel().getAsMention());
                } else {
                    event.getGuild().getAudioManager().openAudioConnection(event.getMember().getVoiceState().getChannel());

                    event.replySuccess("Now in voice: " +  event.getGuild().getSelfMember().getVoiceState().getChannel().getAsMention());
                }
            } else {
                event.replyError("No args!");
            }
        } else {
            if(!event.getGuild().getVoiceChannelById(event.getArgs()).getId().isEmpty()
                    && event.getGuild().getVoiceChannelById(event.getArgs()).getIdLong() != 0)
            {
                if(handler.getPlayingTrack() != null) {
                    handler.getPlayer().setPaused(true);

                    event.getGuild().getAudioManager().openAudioConnection(event.getGuild()
                            .getVoiceChannelById(event.getArgs()));

                    handler.getPlayer().setPaused(false);

                    event.replySuccess("Now in voice: " +  event.getGuild().getSelfMember().getVoiceState().getChannel().getAsMention());
                } else {
                    event.getGuild().getAudioManager().openAudioConnection(event.getGuild()
                            .getVoiceChannelById(event.getArgs()));

                    event.replySuccess("Now in voice: " +  event.getGuild().getSelfMember().getVoiceState().getChannel().getAsMention());
                }
            } else {
                event.replyError("Unknown channel!");
            }
        }
    }
}


