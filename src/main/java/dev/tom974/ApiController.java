package dev.tom974;

import dev.tom974.models.*;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

//    // create contruct function
//    public ApiController(Main mainInstance) {
//        // create instance of DiscordApplication
//        DiscordApplication dcApp = Main.getDiscordInstance();
//    }


    @PostMapping
    public void removeAllRoles(@RequestBody String body) {
//        DiscordApplication.removeAllRoles(guildId)

    }

    @GetMapping(path = "/voiceInfo")
    public HashMap<String, Collection<String>> getVoiceUnits() {
        return DiscordApplication.getVoiceUnits();
    }

    @Autowired
    private static SimpMessagingTemplate simpMessagingTemplate;

    public static void SendUpdateToClients(String channelLeft, String channelJoined, String userId) {
        simpMessagingTemplate.convertAndSend("/porto/receiveMessage", new HashMap<String, String>() {{
            put("channelLeft", channelLeft);
            put("channelJoined", channelJoined);
            put("userId", userId);
        }});
    }

    @GetMapping(path="/getVoiceData")
    // create as follows:
//    {
//        "voiceStates":{
//            "channelId": [
//                "userId",
//                "userid2"
//            ]
//        },
//        "voiceTranslations": {
//            "channelId": "channelName"
//        }
//    }

    public Map<String, Object> getTrackedVoiceChannelsUnits() {
        // i want to return a hashmap with the voiceStates and voiceTranslations
        Map<String, Object> mainMap = new HashMap<>();

        // Define the voiceStates map
//        Map<String, List<String>> voiceStates = new HashMap<>();
//        List<String> userIdsForChannel1 = new ArrayList<>();
//        userIdsForChannel1.add("userId1");
//        userIdsForChannel1.add("userId2");
//        voiceStates.put("channelId1", userIdsForChannel1);

        // Define the voiceTranslations map
//        Map<String, String> voiceTranslations = new HashMap<>();
//        voiceTranslations.put("channelId1", "channelName1");

        // Add the maps to the main map
        mainMap.put("voiceStates", DiscordApplication.getVoiceUnits());
        mainMap.put("voiceTranslations", DiscordApplication.getTrackedVoiceChannels());
        return mainMap;
//        return DiscordApplication.getTrackedVoiceUnits();
    }

    @PostMapping(path = "/changeNickname")
    public void ChangeNickname(@RequestBody ChangeNicknameRequest request) {
        // convert string to long
        boolean success = DiscordApplication.ChangeNickname(request);

        if (success) {
            System.out.println("Nickname changed successfully");
        } else {
            System.out.println("Nickname change failed");
        }
    }

    @PostMapping(path="/switchNickname")
    public void SwitchNickname(@RequestBody SwitchNicknameRequest request) {
        boolean success = DiscordApplication.SwitchNickname(request);

        if (success) {
            System.out.println("Nickname changed successfully");
        } else {
            System.out.println("Nickname change failed");
        }
    }

    @PostMapping(path="/fireUser")
    public void FireUser(@RequestHeader("Authorization") String authHeader, @RequestBody FireUserRequest body) {
        if (!authHeader.equals("Bearer " + System.getenv("API_KEY"))) {
            System.out.println("Invalid API key");
            return;
        }

        DiscordApplication.FireUser(body);
    }

//    @RequestParam String guildId
    @GetMapping(path="/getTrackedVoiceChannels")
    public HashMap<String, String> getTrackedVoiceChannels() {
        return DiscordApplication.getTrackedVoiceChannels();
    }

    @PostMapping(path="/moveChannel")
    public void MoveUser(@RequestBody MoveUserRequest body) {
        DiscordApplication.MoveUser(body);
    }

    @GetMapping(path="/checkForCLS")
    public HashMap<String, Boolean> CheckForCLS(@RequestParam String userId, @RequestParam String guildId) {
        boolean activeAsCLS = DiscordApplication.CheckForCLS(userId, guildId);
        return new HashMap<String, Boolean>() {{
            put("cls", activeAsCLS);
        }};
    }

    @PostMapping(path="/requestLogin")
    public void requestLogin(@RequestBody LoginRequest body) {
        if (body.loginAsLeader) {
            DiscordApplication.addToUnit(body.user, body.info);
            DiscordApplication.addAsLeader(body.user, body.leaderType);
        } else {
//            DiscordApplication dcApp = Main.getDiscordInstance();
//            DiscordApplication.requestLogin(body);
        }
    }

    
}

