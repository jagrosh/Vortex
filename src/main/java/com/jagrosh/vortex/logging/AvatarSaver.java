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
package com.jagrosh.vortex.logging;

import com.typesafe.config.Config;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import javax.imageio.ImageIO;
import net.dv8tion.jda.core.entities.User;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AvatarSaver
{
    private final static BufferedImage NOAVATAR = loadNoAvatar();
    
    private final String userAgent;
    
    public AvatarSaver(Config config)
    {
        userAgent = config.getString("avatar-saver.user-agent");
    }
    
    public byte[] makeAvatarImage(User user, String oldAvatarUrl, String oldAvatarId)
    {
        BufferedImage oldimg;
        if(oldAvatarUrl==null)
            oldimg = imageFromUrl(user.getDefaultAvatarUrl());
        else
        {
            oldimg = imageFromId(user.getIdLong(), oldAvatarId);
            if(oldimg==null)
                oldimg = imageFromUrl(oldAvatarUrl);
        }
        if(oldimg==null)
            oldimg = NOAVATAR;
        BufferedImage newimg = imageFromUrl(user.getEffectiveAvatarUrl());
        if(newimg == null)
            newimg = NOAVATAR;
        else if(user.getAvatarId()!=null) 
            saveImageWithId(newimg, user.getIdLong(), user.getAvatarId());
        BufferedImage combo = new BufferedImage(256,128,BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2d = combo.createGraphics();
        g2d.setColor(Color.BLACK);
        if(oldimg!=null)
        {
            g2d.drawImage(oldimg, 0, 0, 128, 128, null);
        }
        if(newimg!=null)
        {
            g2d.drawImage(newimg, 128, 0, 128, 128, null);
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            ImageIO.write(combo, "png", baos );
            baos.flush();
            return baos.toByteArray();
        }
        catch(IOException ex)
        {
            return null;
        }
        finally
        {
            g2d.dispose();
        }
    }
    
    public BufferedImage imageFromUrl(String url)
    {
        if(url==null)
            return null;
        try
        {
            URL u = new URL(url.replace(".gif", ".png"));
            URLConnection urlConnection = u.openConnection();
            urlConnection.setRequestProperty("user-agent", userAgent);
            return ImageIO.read(urlConnection.getInputStream());
        } 
        catch(IOException|IllegalArgumentException e) 
        {
            return null;
        }
    }
    
    private BufferedImage imageFromId(long id, String avyId)
    {
        try
        {
            return ImageIO.read(location(id, avyId));
        }
        catch(IOException ex)
        {
            return null;
        }
    }
    
    private void saveImageWithId(BufferedImage img, long id, String avyId)
    {
        try
        {
            BufferedImage buf = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
            buf.getGraphics().drawImage(img, 0, 0, 64, 64, null);
            ImageIO.write(buf, "jpg", location(id, avyId));
            buf.getGraphics().dispose();
        }
        catch(IOException ex) {}
    }
    
    private File location(long id, String avyId)
    {
        String dir = "avatars" + File.separator;
        long remainder = id;
        for(int i=16; i>=0; i-=2)
        {
            dir += (remainder / (long)Math.pow(10, i)) + File.separator;
            remainder %= (long)Math.pow(10, i);
            File directory = new File(dir);
            if(!directory.exists())
                directory.mkdir();
        }
        return new File(dir + avyId + ".jpg");
    }
    
    private static BufferedImage loadNoAvatar()
    {
        try
        {
            return ImageIO.read(new File("images"+File.separator+"NoAvatar.png"));
        }
        catch(IOException ex)
        {
            return null;
        }
    }
}
