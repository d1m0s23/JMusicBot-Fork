/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.audio;

import com.github.natanbc.lavadsp.karaoke.KaraokePcmAudioFilter;
import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.github.natanbc.lavadsp.tremolo.TremoloPcmAudioFilter;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.JMusicBot;
import com.jagrosh.jmusicbot.playlist.PlaylistLoader.Playlist;
import com.jagrosh.jmusicbot.settings.RepeatMode;
import com.sedmelluq.discord.lavaplayer.filter.ResamplingPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;

import java.util.*;

import com.jagrosh.jmusicbot.queue.FairQueue;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import java.nio.ByteBuffer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class AudioHandler extends AudioEventAdapter implements AudioSendHandler 
{
    private final Settings settings;
    private Bot bot;
    private final FairQueue<QueuedTrack> queue = new FairQueue<>();
    private final List<AudioTrack> defaultQueue = new LinkedList<>();
    private final Set<String> votes = new HashSet<>();
    private final PlayerManager manager;
    private final AudioPlayer audioPlayer;
    private final long guildId;
    private AudioFrame lastFrame;
    private AudioTrack previous;
    private AudioConfiguration configuration;
    private boolean shouldRebuild;
    private List<AudioFilter> lastChain;
    Logger log = LoggerFactory.getLogger("Audio");

    private static final float[] BASS_BOOST = {
            0.2f, 0.15f, 0.1f, 0.05f, 0.0f, -0.05f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f
    };

    protected AudioHandler(PlayerManager manager, Guild guild, AudioPlayer player)
    {
        this.manager = manager;
        this.bot = bot;
        this.audioPlayer = player;
        this.guildId = guild.getIdLong();
        this.settings = manager.getBot().getSettingsManager().getSettings(guildId);
    }

    public int addTrackToFront(QueuedTrack qtrack)
    {
        if(audioPlayer.getPlayingTrack()==null)
        {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        }
        else
        {
            queue.addAt(0, qtrack);
            return 0;
        }
    }
    
    public int addTrack(QueuedTrack qtrack)
    {
        if(audioPlayer.getPlayingTrack()==null)
        {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        }
        else
            return queue.add(qtrack);
    }
    
    public FairQueue<QueuedTrack> getQueue()
    {
        return queue;
    }
    
    public void stopAndClear()
    {
        queue.clear();
        defaultQueue.clear();
        audioPlayer.stopTrack();
        //current = null;
    }
    
    public boolean isMusicPlaying(JDA jda)
    {
        return guild(jda).getSelfMember().getVoiceState().inVoiceChannel() && audioPlayer.getPlayingTrack()!=null;
    }
    
    public Set<String> getVotes()
    {
        return votes;
    }
    
    public AudioPlayer getPlayer()
    {
        return audioPlayer;
    }
    
    public long getRequester()
    {
        if(audioPlayer.getPlayingTrack()==null || audioPlayer.getPlayingTrack().getUserData(Long.class)==null)
            return 0;
        return audioPlayer.getPlayingTrack().getUserData(Long.class);
    }

    public void setSpeed(Guild guild, double speed) {
        settings.setSpeed(speed);
        updateFilters(getPlayingTrack());
    }
    public void setDepth(Guild guild, float depth) {
        settings.setDepth(depth);
        updateFilters(getPlayingTrack());
    }

    public void enableBassboost(boolean state) {
        settings.setBassboost(state);
        updateFilters(getPlayingTrack());
    }

    public void enableKaraoke(boolean state) {
        settings.setKaraoke(state);
        updateFilters(getPlayingTrack());
    }

    public AudioTrack getPlayingTrack() {
        return getPlayer().getPlayingTrack();
    }

    public AudioTrack getPreviousTrack() {
        return previous;
    }

    public boolean getBassboostState() {
        return settings.getBassBoost();
    }

    public boolean playFromDefault()
    {
        if(!defaultQueue.isEmpty())
        {
            audioPlayer.playTrack(defaultQueue.remove(0));
            return true;
        }
        Settings settings = manager.getBot().getSettingsManager().getSettings(guildId);
        if(settings==null || settings.getDefaultPlaylist()==null)
            return false;
        
        Playlist pl = manager.getBot().getPlaylistLoader().getPlaylist(settings.getDefaultPlaylist());
        if(pl==null || pl.getItems().isEmpty())
            return false;
        pl.loadTracks(manager, (at) -> 
        {
            if(audioPlayer.getPlayingTrack()==null)
                audioPlayer.playTrack(at);
            else
                defaultQueue.add(at);
        }, () -> 
        {
            if(pl.getTracks().isEmpty() && !manager.getBot().getConfig().getStay())
                manager.getBot().closeAudioConnection(guildId);
        });
        return true;
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        track.stop();
    }

    // Audio Events
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) 
    {
        if(endReason==AudioTrackEndReason.FINISHED) {
            previous = track;

            switch (manager.getBot().getSettingsManager().getSettings(guildId).getRepeatMode()) {
                case TRACK: queue.add(new QueuedTrack(track.makeClone(),
                        track.getUserData(Long.class) == null ? 0L : track.getUserData(Long.class)));
                case QUEUE: queue.addAt(queue.size(), new QueuedTrack(track.makeClone(),
                        track.getUserData(Long.class) == null ? 0L : track.getUserData(Long.class)));
            }
        }

        if(queue.isEmpty())
        {
            if(!playFromDefault())
            {
                manager.getBot().getNowplayingHandler().onTrackUpdate(guildId, null, this);
                if(!manager.getBot().getConfig().getStay())
                    manager.getBot().closeAudioConnection(guildId);
                // unpause, in the case when the player was paused and the track has been skipped.
                // this is to prevent the player being paused next time it's being used.
                player.setPaused(false);
            }
        }
        else
        {
            if(Objects.equals(manager.getBot().getSettingsManager().getSettings(guildId).getRepeatMode(), "queue")) {
                int i = 0;
                QueuedTrack qt = queue.get(i++);
                player.playTrack(qt.getTrack());
            } else {
                QueuedTrack qt = queue.pull();
                player.playTrack(qt.getTrack());
            }
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) 
    {
        updateFilters(track);
        votes.clear();
        manager.getBot().getNowplayingHandler().onTrackUpdate(guildId, track, this);
    }

    // Formatting
    public Message getNowPlaying(JDA jda)
    {
        if(isMusicPlaying(jda))
        {
            Guild guild = guild(jda);
            AudioTrack track = audioPlayer.getPlayingTrack();
            MessageBuilder mb = new MessageBuilder();
            mb.append(FormatUtil.filter(manager.getBot().getConfig().getSuccess()+" **Now Playing in "+guild.getSelfMember().getVoiceState().getChannel().getAsMention()+"...**"));
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(guild.getSelfMember().getColor());
            if(getRequester() != 0)
            {
                User u = guild.getJDA().getUserById(getRequester());
                if(u==null)
                    eb.setAuthor("Unknown (ID:"+getRequester()+")", null, null);
                else
                    eb.setAuthor(u.getName()+"#"+u.getDiscriminator(), null, u.getEffectiveAvatarUrl());
            }

            try 
            {
                eb.setTitle(track.getInfo().title, track.getInfo().uri);
            }
            catch(Exception e) 
            {
                eb.setTitle(track.getInfo().title);
            }

            if(track instanceof YoutubeAudioTrack && manager.getBot().getConfig().useNPImages())
            {
                eb.setThumbnail("https://img.youtube.com/vi/"+track.getIdentifier()+"/mqdefault.jpg");
            }
            
            if(track.getInfo().author != null && !track.getInfo().author.isEmpty())
                eb.setFooter("Source: " + track.getInfo().author, null);

            double progress = (double)audioPlayer.getPlayingTrack().getPosition()/track.getDuration();
            eb.setDescription((audioPlayer.isPaused() ? JMusicBot.PAUSE_EMOJI : JMusicBot.PLAY_EMOJI)
                    + " "+FormatUtil.progressBar(progress)
                    + " `[" + FormatUtil.formatTime(track.getPosition()) + "/" + FormatUtil.formatTime(track.getDuration()) + "]` "
                    + FormatUtil.volumeIcon(audioPlayer.getVolume()));
            
            return mb.setEmbed(eb.build()).build();
        }
        else return null;
    }
    
    public Message getNoMusicPlaying(JDA jda)
    {
        Guild guild = guild(jda);
        return new MessageBuilder()
                .setContent(FormatUtil.filter(manager.getBot().getConfig().getSuccess()+" **Now Playing...**"))
                .setEmbed(new EmbedBuilder()
                .setTitle("No music playing")
                .setDescription(JMusicBot.STOP_EMOJI+" "+FormatUtil.progressBar(-1)+" "+FormatUtil.volumeIcon(audioPlayer.getVolume()))
                .setColor(guild.getSelfMember().getColor())
                .build()).build();
    }
    
    public String getTopicFormat(JDA jda)
    {
        if(isMusicPlaying(jda))
        {
            long userid = getRequester();
            AudioTrack track = audioPlayer.getPlayingTrack();
            String title = track.getInfo().title;
            if(title==null || title.equals("Unknown Title"))
                title = track.getInfo().uri;
            return "**"+title+"** ["+(userid==0 ? "autoplay" : "<@"+userid+">")+"]"
                    + "\n" + (audioPlayer.isPaused() ? JMusicBot.PAUSE_EMOJI : JMusicBot.PLAY_EMOJI) + " "
                    + "[" + FormatUtil.formatTime(track.getDuration()) + "] "
                    + FormatUtil.volumeIcon(audioPlayer.getVolume());
        }
        else return "No music playing " + JMusicBot.STOP_EMOJI + " " + FormatUtil.volumeIcon(audioPlayer.getVolume());
    }
    
    // Audio Send Handler methods
    /*@Override
    public boolean canProvide() 
    {
        if (lastFrame == null)
            lastFrame = audioPlayer.provide();

        return lastFrame != null;
    }

    @Override
    public byte[] provide20MsAudio() 
    {
        if (lastFrame == null) 
            lastFrame = audioPlayer.provide();

        byte[] data = lastFrame != null ? lastFrame.getData() : null;
        lastFrame = null;

        return data;
    }*/
    
    @Override
    public boolean canProvide() 
    {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public ByteBuffer provide20MsAudio() 
    {
        return ByteBuffer.wrap(lastFrame.getData());
    }

    @Override
    public boolean isOpus() 
    {
        return true;
    }
    
    
    // Private methods
    private Guild guild(JDA jda)
    {
        return jda.getGuildById(guildId);
    }

    // equalizer
    private List<AudioFilter> getFiltersOrRebuild(AudioTrack audioTrack, AudioDataFormat audioDataFormat, UniversalPcmAudioFilter downstream) {
        if (shouldRebuild) {
            lastChain = buildChain(audioTrack, audioDataFormat, downstream);
            shouldRebuild = false;
        }

        return lastChain;
    }

    public boolean FiltersEnabled() {
        return settings.getBassBoost() || settings.getKaraoke() || settings.getNightcore() || settings.getSpeed() != 1 || settings.getDepth() != 1;
    }

    public void updateFilters(AudioTrack track) {
        if (FiltersEnabled()) {
            shouldRebuild = true;
            audioPlayer.setFilterFactory(this::getFiltersOrRebuild);
        } else {
            audioPlayer.setFilterFactory(null);
        }
    }

    private List<AudioFilter> buildChain(AudioTrack audioTrack, AudioDataFormat format, UniversalPcmAudioFilter downstream) {
        List<AudioFilter> filterList = new ArrayList<>();
        FloatPcmAudioFilter filter = downstream;
        Equalizer equalizer = new Equalizer(format.channelCount, filter);
        TimescalePcmAudioFilter timescale = new TimescalePcmAudioFilter(filter, format.channelCount, format.sampleRate);
        KaraokePcmAudioFilter karaokeFilter = new KaraokePcmAudioFilter(filter, format.channelCount, format.sampleRate);
        TremoloPcmAudioFilter tremolo = new TremoloPcmAudioFilter(filter, format.channelCount, format.sampleRate);


        // bassboost
        if(settings.getBassBoost()) {
            for (int i = 0; i < BASS_BOOST.length; i++) {
                equalizer.setGain(i, BASS_BOOST[i] + 2);
            }

            for (int i = 0; i < BASS_BOOST.length; i++) {
                equalizer.setGain(i, -BASS_BOOST[i] + 1);
            }
            filter = equalizer;
            filterList.add(equalizer);
            settings.setBassboost(true);
        } else {
            try {
                equalizer.flush();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            filter = null;
            filterList.remove(equalizer);
            settings.setBassboost(false);
        }

        if(settings.getSpeed() != timescale.getSpeed()) {
            timescale.setSpeed(settings.getSpeed());
            filter = timescale;
            filterList.add(timescale);
        } else if(settings.getSpeed() == 1) {
            timescale.flush();
        }

        if(settings.getKaraoke()) {
            if(!manager.getBot().getConfig().getKaraokeBand().equals("NONE") && !manager.getBot().getConfig().getKaraokeLvl().equals("NONE") && !manager.getBot().getConfig().getKaraokeMono().equals("NONE") && !manager.getBot().getConfig().getKaraokeWidth().equals("NONE")) {
                karaokeFilter.setLevel(Float.parseFloat(manager.getBot().getConfig().getKaraokeLvl()))
                        .setMonoLevel(Float.parseFloat(manager.getBot().getConfig().getKaraokeMono()))
                        .setFilterBand(Float.parseFloat(manager.getBot().getConfig().getKaraokeBand()))
                        .setFilterWidth(Float.parseFloat(manager.getBot().getConfig().getKaraokeWidth()));
                filter = karaokeFilter;
                filterList.add(karaokeFilter);
                settings.setKaraoke(true);
            } else {
                karaokeFilter.setLevel(1f)
                        .setMonoLevel(1f)
                        .setFilterBand(220f)
                        .setFilterWidth(100f);
                filter = karaokeFilter;
                filterList.add(karaokeFilter);
                settings.setKaraoke(true);
            }
        } else {
            filter = null;
            filterList.remove(karaokeFilter);
            settings.setKaraoke(false);
        }

        Collections.reverse(filterList);
        return filterList;
    }

}
