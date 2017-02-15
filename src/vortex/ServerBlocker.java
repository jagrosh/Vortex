/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex;

import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ServerBlocker extends ListenerAdapter {

    private final AutoMod automod;
    
    public ServerBlocker(AutoMod automod)
    {
        this.automod = automod;
    }
    
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        automod.onBlockedGuildJoin(event);
    }
}
