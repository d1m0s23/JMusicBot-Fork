/*
 * Copyright 2018 John Grosh (jagrosh)
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
package com.jagrosh.jmusicbot;

import com.jagrosh.jmusicbot.entities.Prompt;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.jagrosh.jmusicbot.utils.OtherUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.typesafe.config.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

/**
 * 
 * 
 * @author John Grosh (jagrosh)
 */
public class BotConfig
{
    private final Prompt prompt;
    private final static String CONTEXT = "Config";
    private final static String START_TOKEN = "/// START OF JMUSICBOT CONFIG ///";
    private final static String END_TOKEN = "/// END OF JMUSICBOT CONFIG ///";
    
    private Path path = null;
    private String token;
    private String prefix;
    private String altprefix;
    private String helpWord;
    private String playlistsFolder;
    private String successEmoji;
    private String warningEmoji;
    private String errorEmoji;
    private String loadingEmoji;
    private String searchingEmoji;
    private boolean stayInChannel;
    private boolean songInGame;
    private boolean npImages;
    private boolean updatealerts;
    private boolean useEval;
    private int maxvolume;
    private String depth;
    private String karaokeLvl, karaokeMono, karaokeWidth, karaokeBand;
    private String speed;
    private long owner, maxSeconds, aloneTimeUntilStop;
    private OnlineStatus status;
    private Activity game;
    private Config aliases, transforms;

    private boolean valid = false;
    
    public BotConfig(Prompt prompt)
    {
        this.prompt = prompt;
    }
    
    public void load()
    {
        valid = false;
        
        // read config from file
        try 
        {
            // get the path to the config, default config.txt
            path = OtherUtil.getPath(System.getProperty("config.file", System.getProperty("config", "config.txt")));
            if(path.toFile().exists())
            {
                if(System.getProperty("config.file") == null)
                    System.setProperty("config.file", System.getProperty("config", "config.txt"));
                ConfigFactory.invalidateCaches();
            }
            
            // load in the config file, plus the default values
            //Config config = ConfigFactory.parseFile(path.toFile()).withFallback(ConfigFactory.load());
            Config config = ConfigFactory.load();
            
            // set values
            token = config.getString("token");
            prefix = config.getString("prefix");
            altprefix = config.getString("altprefix");
            helpWord = config.getString("help");
            owner = config.getLong("owner");
            successEmoji = config.getString("success");
            warningEmoji = config.getString("warning");
            errorEmoji = config.getString("error");
            loadingEmoji = config.getString("loading");
            searchingEmoji = config.getString("searching");
            game = OtherUtil.parseGame(config.getString("game"));
            status = OtherUtil.parseStatus(config.getString("status"));
            stayInChannel = config.getBoolean("stayinchannel");
            songInGame = config.getBoolean("songinstatus");
            npImages = config.getBoolean("npimages");
            updatealerts = config.getBoolean("updatealerts");
            useEval = config.getBoolean("eval");
            maxSeconds = config.getLong("maxtime");
            aloneTimeUntilStop = config.getLong("alonetimeuntilstop");
            playlistsFolder = config.getString("playlistsfolder");
            aliases = config.getConfig("aliases");
            transforms = config.getConfig("transforms");
            maxvolume = config.getInt("maxvolume");
            depth = config.getString("depth");
            speed = config.getString("speed");
            karaokeBand = config.getString("karaokeband");
            karaokeMono = config.getString("karaokemono");
            karaokeWidth = config.getString("karaokewidth");
            karaokeLvl = config.getString("karaokelvl");

            // we may need to write a new config file
            boolean write = false;

            // validate bot token
            if(token==null || token.isEmpty() || token.equalsIgnoreCase("BOT_TOKEN_HERE"))
            {
                token = prompt.prompt("Please provide a bot token."
                        + "\nBot Token: ");
                if(token==null)
                {
                    prompt.alert(Prompt.Level.WARNING, CONTEXT, "No token provided! Exiting.\n\nConfig Location: " + path.toAbsolutePath().toString());
                    return;
                }
                else
                {
                    write = true;
                }
            }
            
            // validate bot owner
            if(owner<=0)
            {
                try
                {
                    owner = Long.parseLong(prompt.prompt("Owner ID was missing, or the provided owner ID is not valid."
                            + "\nOwner User ID: "));
                }
                catch(NumberFormatException | NullPointerException ex)
                {
                    owner = 0;
                }
                if(owner<=0)
                {
                    prompt.alert(Prompt.Level.ERROR, CONTEXT, "Invalid User ID! Exiting.\n\nConfig Location: " + path.toAbsolutePath().toString());
                    return;
                }
                else
                {
                    write = true;
                }
            }
            
            if(write)
                writeToFile();
            
            // if we get through the whole config, it's good to go
            valid = true;
        }
        catch (ConfigException ex)
        {
            prompt.alert(Prompt.Level.ERROR, CONTEXT, ex + ": " + ex.getMessage() + "\n\nConfig Location: " + path.toAbsolutePath().toString());
        }
    }

    private void writeToFile()
    {
        String original = OtherUtil.loadResource(this, "/reference.conf");
        byte[] bytes;
        if(original==null)
        {
            bytes = ("token = "+token+"\r\nowner = "+owner).getBytes();
        }
        else
        {
            bytes = original.substring(original.indexOf(START_TOKEN)+START_TOKEN.length(), original.indexOf(END_TOKEN))
                    .replace("BOT_TOKEN_HERE", token)
                    .replace("0 // OWNER ID", Long.toString(owner))
                    .trim().getBytes();
        }
        try
        {
            Files.write(path, bytes);
        }
        catch(IOException ex)
        {
            prompt.alert(Prompt.Level.WARNING, CONTEXT, "Failed to write new config options to config.txt: "+ex
                    + "\nPlease make sure that the files are not on your desktop or some other restricted area.\n\nConfig Location: "
                    + path.toAbsolutePath().toString());
        }
    }
    
    public boolean isValid()
    {
        return valid;
    }
    
    public String getConfigLocation()
    {
        return path.toFile().getAbsolutePath();
    }
    
    public String getPrefix()
    {
        return prefix;
    }
    
    public String getAltPrefix()
    {
        return "NONE".equalsIgnoreCase(altprefix) ? null : altprefix;
    }
    
    public String getToken()
    {
        return token;
    }

    public String getKaraokeLvl() { return karaokeLvl; }

    public String getKaraokeMono() { return karaokeMono; }

    public String getKaraokeWidth() { return karaokeWidth; }

    public String getKaraokeBand() { return karaokeBand; }

    public long getOwnerId()
    {
        return owner;
    }

    public int getMaxVolume() { return maxvolume; }

    public String getDepth() { return depth; }

    public String getSpeed() { return speed; }

    public String getSuccess()
    {
        return successEmoji;
    }
    
    public String getWarning()
    {
        return warningEmoji;
    }
    
    public String getError()
    {
        return errorEmoji;
    }
    
    public String getLoading()
    {
        return loadingEmoji;
    }
    
    public String getSearching()
    {
        return searchingEmoji;
    }
    
    public Activity getGame()
    {
        return game;
    }
    
    public OnlineStatus getStatus()
    {
        return status;
    }
    
    public String getHelp()
    {
        return helpWord;
    }
    
    public boolean getStay()
    {
        return stayInChannel;
    }
    
    public boolean getSongInStatus()
    {
        return songInGame;
    }
    
    public String getPlaylistsFolder()
    {
        return playlistsFolder;
    }

    public boolean useUpdateAlerts()
    {
        return updatealerts;
    }
    
    public boolean useEval()
    {
        return useEval;
    }
    
    public boolean useNPImages()
    {
        return npImages;
    }
    
    public long getMaxSeconds()
    {
        return maxSeconds;
    }
    
    public String getMaxTime()
    {
        return FormatUtil.formatTime(maxSeconds * 1000);
    }

    public long getAloneTimeUntilStop()
    {
        return aloneTimeUntilStop;
    }
    
    public boolean isTooLong(AudioTrack track)
    {
        if(maxSeconds<=0)
            return false;
        return Math.round(track.getDuration()/1000.0) > maxSeconds;
    }

    public String[] getAliases(String command)
    {
        try
        {
            return aliases.getStringList(command).toArray(new String[0]);
        }
        catch(NullPointerException | ConfigException.Missing e)
        {
            return new String[0];
        }
    }
    
    public Config getTransforms()
    {
        return transforms;
    }
}
