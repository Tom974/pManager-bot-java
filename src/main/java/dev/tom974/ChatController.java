package dev.tom974;

import dev.tom974.models.PortoChannelUpdate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
// import org.springframework.web.bind.annotation.CrossOrigin;

// @CrossOrigin(origins = "https://localhost:5173")
@Controller
public class ChatController {
    @MessageMapping("/porto/sendMessage")
    @SendTo("/porto/receiveMessage")
    public PortoChannelUpdate sendMessage(PortoChannelUpdate update) {
//        User sender = userService.getUserByToken(chatMessage.sender);
//        Timestamp now = new Timestamp(System.currentTimeMillis());
//        System.out.println("Message received: " + chatMessage.content);
        System.out.println("Received message from the websocket!");
        return update;
//        chatRepo.save(new Chat(now, now, null, sender, chatMessage.content));
    }


    
}
