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
package vortex.utils;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import org.json.JSONObject;
import vortex.entities.Invite;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class RestUtil {
    
    public static Invite resolveInvite(String code)
    {
        try 
        {
            HttpResponse<JsonNode> result = Unirest.get("https://discordapp.com/api/invites/"+code).asJson();
            JSONObject obj = result.getBody().getObject();
            return new Invite(obj.getJSONObject("guild").getString("id"), obj.getJSONObject("guild").getString("name"));
        }
        catch(Exception e)
        {
            return null;
        }
    }
    
}
