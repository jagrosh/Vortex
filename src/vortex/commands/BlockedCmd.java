/*
 * Copyright 2016 John Grosh (jagrosh).
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
package vortex.commands;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import me.jagrosh.jdautilities.commandclient.Command;
import me.jagrosh.jdautilities.commandclient.CommandEvent;
import me.jagrosh.jdautilities.menu.buttonmenu.ButtonMenuBuilder;
import me.jagrosh.jdautilities.menu.pagination.Paginator;
import me.jagrosh.jdautilities.menu.pagination.PaginatorBuilder;
import me.jagrosh.jdautilities.waiter.EventWaiter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.utils.PermissionUtil;
import vortex.AutoMod;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class BlockedCmd extends Command {

    private final AutoMod automod;
    private final PaginatorBuilder builder;
    private final ButtonMenuBuilder confirmation;
    private final String CANCEL = "\u274C";
    private final String CONFIRM = "\u2611";
    public BlockedCmd(AutoMod automod, EventWaiter waiter)
    {
        this.automod = automod;
        this.name = "blocked";
        this.help = "shows blocked members";
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION};
        this.userPermissions = new Permission[]{Permission.ADMINISTRATOR};
        this.builder = new PaginatorBuilder()
                .setColumns(1)
                .setItemsPerPage(35)
                .showPageNumbers(true)
                .waitOnSinglePage(false)
                .setFinalAction(m -> {try{m.clearReactions().queue();}catch(Exception e){}})
                .setTimeout(1, TimeUnit.MINUTES)
                .setEventWaiter(waiter)
                ;
        this.confirmation = new ButtonMenuBuilder()
                .setChoices(CONFIRM, CANCEL)
                .setEventWaiter(waiter)
                .setTimeout(1, TimeUnit.MINUTES)
                ;
    }
    
    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().toLowerCase().startsWith("kickall"))
        {
            String id = event.getArgs().substring(7).trim();
            Guild blockedGuild = automod.findGuildById(id);
            if(blockedGuild==null)
            {
                event.reply(event.getClient().getError()+" That guild (`"+id+"`) was not found!");
                return;
            }
            List<Member> toKick = automod.getBlockedKickableMembers(event.getGuild(), id);
            List<Member> kicking  = toKick.stream().filter(m -> !m.getUser().isBot() &&
                        PermissionUtil.canInteract(event.getSelfMember(), m) && 
                        !m.getRoles().stream().anyMatch(r -> r.getName().toLowerCase().equals("vortexshield")) &&
                        !PermissionUtil.checkPermission(event.getGuild(), m, Permission.MESSAGE_MANAGE) &&
                        !PermissionUtil.checkPermission(event.getGuild(), m, Permission.KICK_MEMBERS) &&
                        !PermissionUtil.checkPermission(event.getGuild(), m, Permission.BAN_MEMBERS) &&
                        !PermissionUtil.checkPermission(event.getGuild(), m, Permission.MANAGE_SERVER)).collect(Collectors.toList());
            if(kicking.isEmpty())
            {
                event.reply(event.getClient().getError()+" There is nobody to kick!");
                return;
            }
            confirmation.setText(event.getClient().getWarning()+" This will kick __"+kicking.size()+"__ members that are on guild `"+blockedGuild.getName()+"`. Continue?")
                    .setAction(e2 -> {
                        if(e2.getName().equals(CONFIRM))
                        {
                            new Thread(){
                                @Override
                                public void run() {
                                    String str = "";
                                    for(int i=0; i<kicking.size(); i++)
                                    {
                                        str += kicking.get(i).getAsMention()+" ";
                                        try{
                                            kicking.get(i).getUser().openPrivateChannel().complete().sendMessage("You have been kicked from **"
                                                    +event.getGuild().getName()+"** for being on the server **"+blockedGuild.getName()+"**. You may rejoin after you leave **"
                                                    +blockedGuild.getName()+"**").complete();
                                        }catch(Exception e){}
                                        if(i!=kicking.size()-1)
                                            event.getGuild().getController().kick(kicking.get(i)).complete();
                                        else
                                        {
                                            String list = str;
                                            event.getGuild().getController().kick(kicking.get(i)).complete();
                                            event.getChannel().sendMessage(new MessageBuilder()
                                                    .append(event.getClient().getSuccess()+" Successfully kicked: ")
                                                    .setEmbed(new EmbedBuilder().setDescription(list).build())
                                                    .build()).complete();
                                        }
                                    }
                                }
                            }.start();
                        }
                    }).setUsers(event.getAuthor())
                    .build().display(event.getChannel());
            return;
        }
        List<String> list = automod.getBlockedMembers(event.getGuild());
        if(list.isEmpty())
        {
            event.reply(event.getClient().getWarning()+" There are no blocked users here!");
            return;
        }
        builder.setText(event.getClient().getSuccess()+" "+list.size()+" blocked users:")
                .setColor(event.getSelfMember().getColor())
                .setUsers(event.getAuthor());
        builder.clearItems();
        list.forEach(str -> builder.addItems(str));
        Paginator p = builder.build();
        p.display(event.getChannel());
    }
    
}
