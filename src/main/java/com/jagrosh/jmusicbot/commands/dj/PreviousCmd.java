package com.jagrosh.jmusicbot.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.DJCommand;
import net.dv8tion.jda.api.entities.GuildVoiceState;

public class PreviousCmd extends DJCommand {
    public PreviousCmd(Bot bot) {
        super(bot);
        this.name = "previous";
        this.help = "plays the previous track";
        this.aliases = bot.getConfig().getAliases(this.name);
    }

    @Override
    public void doCommand(CommandEvent event) {
        AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
        GuildVoiceState userState = event.getMember().getVoiceState();

        if(handler.getPreviousTrack() != null) {
            if(handler.getNowPlaying(bot.getJDA()) != null) {
                try {
                    handler.getQueue().add(new QueuedTrack(handler.getPreviousTrack().makeClone(),
                            handler.getPreviousTrack().getUserData(Long.class) == null ? 0L
                                    : handler.getPreviousTrack().getUserData(Long.class)));

                    handler.getPlayer().stopTrack();
                } finally {
                    event.replySuccess("Previous loaded!");
                }
            } else {
                try {
                    if(userState.inVoiceChannel()) {

                        event.getGuild().getAudioManager().openAudioConnection(userState.getChannel());

                        handler.getQueue().add(new QueuedTrack(handler.getPreviousTrack().makeClone(),
                                handler.getPreviousTrack().getUserData(Long.class) == null ? 0L
                                        : handler.getPreviousTrack().getUserData(Long.class)));

                        handler.getPlayer().stopTrack();
                    } else {
                        event.replyError("You not in voice channel!");
                    }
                } finally {
                    event.replySuccess("Previous loaded!");
                }
            }
        } else {
            event.replyError("Previous is null!");
        }
    }
}
