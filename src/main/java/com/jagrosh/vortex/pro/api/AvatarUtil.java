package com.jagrosh.vortex.pro.api;

import com.jagrosh.vortex.pro.ProFeature;
import net.dv8tion.jda.core.entities.User;

@ProFeature("com.jagrosh.vortex.pro.AvatarUtil")
public interface AvatarUtil
{
    byte[] makeAvatarImage(User user, String oldAvatarUrl, String oldAvatarId);
}
