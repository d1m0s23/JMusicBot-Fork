package com.jagrosh.jmusicbot.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.DJCommand;

public class SpeedCmd extends DJCommand {
    public SpeedCmd(Bot bot) {
        super(bot);
        this.name = "speed";
        this.help = "changes track speed";
        this.arguments = "1|2.5";
        this.aliases = bot.getConfig().getAliases(this.name);
    }

    @Override
    public void doCommand(CommandEvent event) {
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        double args = 0;

        try {
            args = Double.parseDouble(event.getArgs());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if(event.getArgs().length() != 0) {
            if(!Double.isNaN(args)) {
                if(args <= 0) {
                    event.replyError("Number must be larger than `0`");
                } else {
                    handler.setSpeed(event.getGuild(), Double.parseDouble(event.getArgs()));
                    event.replySuccess("Speed now: " + "`" + args + "`");
                }
            } else {
                event.replyError("Only numbers supported!");
            }

        } else {
            event.replySuccess("Speed now:" + "`" + bot.getSettingsManager().getSettings(event.getGuild().getIdLong()).getSpeed() + "`");
        }

    }
}
