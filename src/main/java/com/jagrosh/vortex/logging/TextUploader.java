/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex.logging;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class TextUploader
{
    /**
     * Alright, I'm going to write a bit here because I have a feeling that some
     * people will wonder about this. The purpose of this class is to utilize
     * Discord's persistent and encrypted message channels as storage for
     * message logs. 
     */
    private final Logger LOG = LoggerFactory.getLogger("Upload");
    private final JDA altBot;
    private final long categoryId;
    private final long guildId;
    private int index = 0;
    
    public TextUploader(JDA altBot, long guildId, long categoryId)
    {
        this.altBot = altBot;
        this.guildId = guildId;
        this.categoryId = categoryId;
    }

    public synchronized void upload(String content, String filename, Result done)
    {
        Guild guild = altBot.getGuildById(guildId);
        if(guild==null)
            return;
        Category category = guild.getCategoryById(categoryId);
        List<TextChannel> list = category.getTextChannels();
        AtomicBoolean failed = new AtomicBoolean();
        if (list.size() == 0)
        {
            category.createTextChannel("uploader").queue(s -> {}, f -> {
                LOG.error("Failed to create text channel: " + f);
                failed.set(true);
            });
        }
        if (failed.get())
            return;

        list.get(index % list.size()).sendFiles(FileUpload.fromData(content.getBytes(StandardCharsets.UTF_8), filename+".txt")).queue(
                m -> done.consume(
                        "https://txt.discord.website?txt="+m.getAttachments().get(0).getUrl().substring(m.getAttachments().get(0).getUrl().indexOf("s/")+2, m.getAttachments().get(0).getUrl().length()-4), 
                        m.getAttachments().get(0).getUrl()), 
                f -> LOG.error("Failed to upload: "+f));
        index++;
    }
    
    public interface Result
    {
        void consume(String view, String download);
    }
}