package com.jagrosh.jmusicbot.listeners;

import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.settings.RepeatMode;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class InteractionListener extends ListenerAdapter {
    private Bot bot;

    public InteractionListener(Bot bot)
    {
        this.bot = bot;
    }

    public void onButtonClick(ButtonClickEvent event) {
        AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
        int volume = bot.getSettingsManager().getSettings(event.getGuild().getIdLong()).getVolume();

        assert handler != null;
        if(handler.getPlayer().getPlayingTrack() == null) {
            event.getInteraction().reply("No track playing").queue();
        } else {
            switch (event.getComponentId()) {
                case "play:volume_minus":
                    bot.getSettingsManager().getSettings(event.getGuild().getIdLong()).setVolume(volume -= 10);
                    handler.getPlayer().setVolume(volume -=  10);
                    event.reply("Volume now: " + "`" +  handler.getPlayer().getVolume() + "`").queue();
                    break;
                case "play:volume_plus":
                    bot.getSettingsManager().getSettings(event.getGuild().getIdLong()).setVolume(volume += 10);
                    handler.getPlayer().setVolume(volume += 10);
                    event.reply("Volume now: " +  "`" +  handler.getPlayer().getVolume() + "`").queue();
                    break;
                case "play:stop":
                    handler.getPlayer().stopTrack();
                    event.reply("Stopped").queue();
                    break;
                case "play:play_pause":
                    if (handler.getPlayer().isPaused()) {
                        handler.getPlayer().setPaused(false);
                        event.reply("Unpause!").queue();
                        break;
                    } else {
                        handler.getPlayer().setPaused(true);
                        event.reply( "Paused!").queue();
                        break;
                    }
                case "play:skip":
                    handler.getPlayer().getPlayingTrack().stop();
                    event.reply("Skipped!").queue();
                    break;
                case "play:repeat":
                    if(bot.getSettingsManager().getSettings(event.getGuild().getIdLong()).getRepeatMode().equals(RepeatMode.OFF)) {
                        bot.getSettingsManager().getSettings(event.getGuild().getIdLong()).setRepeatMode(RepeatMode.TRACK);
                        event.reply("Now repeat is: " + "**" + RepeatMode.TRACK.getName() + "**").queue();
                        break;
                    } else if(bot.getSettingsManager().getSettings(event.getGuild().getIdLong()).getRepeatMode().equals(RepeatMode.TRACK)) {
                        bot.getSettingsManager().getSettings(event.getGuild().getIdLong()).setRepeatMode(RepeatMode.QUEUE);
                        event.reply("Now repeat is: " + "**" + RepeatMode.QUEUE.getName() + "**").queue();
                        break;
                    } else if(bot.getSettingsManager().getSettings(event.getGuild().getIdLong()).getRepeatMode().equals(RepeatMode.QUEUE)) {
                        bot.getSettingsManager().getSettings(event.getGuild().getIdLong()).setRepeatMode(RepeatMode.OFF);
                        event.reply("Now repeat is: " + "**" + RepeatMode.OFF.getName() + "**").queue();
                        break;
                    }
            }
        }
    }
}
