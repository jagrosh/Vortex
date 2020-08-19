/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex.commands.tools;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandErrorException;
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandWarningException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Invite;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class InvitepruneCmd extends Command
{
    private final static String CANCEL = "\u274C"; // ❌
    private final static String CONFIRM = "\u2611"; // ☑
    
    private final Vortex vortex;
    
    public InvitepruneCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "inviteprune";
        this.arguments = "[max uses]";
        this.help = "deletes invites with up to a certain number of uses";
        this.category = new Category("Tools");
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        this.botPermissions = new Permission[]{Permission.MANAGE_SERVER};
        this.guildOnly = true;
        this.cooldown = 60*5; // 5 minute cooldown for safety
    }
    
    @Override
    protected void execute(CommandEvent event)
    {
        int uses;
        if(event.getArgs().isEmpty())
            uses = 1;
        else try
        {
            uses = Integer.parseInt(event.getArgs());
        }
        catch(NumberFormatException ex)
        {
            throw new CommandErrorException("`"+event.getArgs()+"` is not a valid integer!");
        }
        if(uses<0 || uses>50)
            throw new CommandWarningException("Maximum uses must be at least 0 and no larger than 50");
        if(uses>10)
            waitForConfirmation(event, "This will delete all invites with "+uses+" or fewer uses.", () -> pruneInvites(uses, event));
        else
            pruneInvites(uses, event);
    }
    
    private void pruneInvites(int uses, CommandEvent event)
    {
        event.getChannel().sendTyping().queue();
        event.getGuild().getInvites().queue(list -> 
        {
            List<Invite> toPrune = list.stream().filter(i -> i.getInviter()!=null && !i.getInviter().isBot() && i.getUses()<=uses).collect(Collectors.toList());
            toPrune.forEach(i -> i.delete().queue());
            event.replySuccess("Deleting `"+toPrune.size()+"` invites with `"+uses+"` or fewer uses.");
        });
    }
    
    private void waitForConfirmation(CommandEvent event, String message, Runnable confirm)
    {
        new ButtonMenu.Builder()
                .setChoices(CONFIRM, CANCEL)
                .setEventWaiter(vortex.getEventWaiter())
                .setTimeout(1, TimeUnit.MINUTES)
                .setText(event.getClient().getWarning()+" "+message+"\n\n"+CONFIRM+" Continue\n"+CANCEL+" Cancel")
                .setFinalAction(m -> m.delete().queue(s->{}, f->{}))
                .setUsers(event.getAuthor())
                .setAction(re ->
                {
                    if(re.getName().equals(CONFIRM))
                        confirm.run();
                }).build().display(event.getChannel());
    }
}
