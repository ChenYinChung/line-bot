package mps.linebot.controller;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.Broadcast;
import com.linecorp.bot.model.Multicast;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.response.BotApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController("/")
public class HelloController {
  @Autowired private LineMessagingClient lineMessagingClient;

  @Value("${line.bot.channel-token}")
  String channelToken;

  String userId = "U15bfc8c8105dfa155b9f5ae32e4a31a0";
  String roomId = "Ra1ba6323c881c0cba849a69ae42784fd";

  @GetMapping("/hello")
  public String helloGradle() {
    return "Hello Gradle!";
  }

  /**
   * for direct assign userId / roomId
   * 1. to : 主動通知需要UserId(GroupId群發)
   * 2. channelToken
   * 3. message */
  @GetMapping("/push")
  public void pushMessage() {
    try {
      PushMessage pushMessage = new PushMessage(userId, new TextMessage("HelloController push 回覆"));
      BotApiResponse response = lineMessagingClient.pushMessage(pushMessage).get();
      log.info("Sent messages: {}", response);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * send message for assign users
   */
  @GetMapping("/multicast")
  public void multicastMessage() {
    try {

      Set<String> userIds = new HashSet<>();
      userIds.add(userId);
      Multicast multicast = new Multicast(userIds, new TextMessage("HelloController multicast for assign users 回覆"));

      BotApiResponse response = lineMessagingClient.multicast(multicast).get();
      log.info("Sent messages: {}", response);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * send message for each other
   */
  @GetMapping("/broadcast")
  public void broadcastMessage() {
    try {
//
//      Set<String> userIds = new HashSet<>();
//      userIds.add(userId);
//      Multicast multicast = new Multicast(userIds, new TextMessage("HelloController broadcastMessage 回覆"));
      TextMessage textMessage = new TextMessage("HelloController broadcastMessage for all users 回覆");
      Broadcast broadcast = new Broadcast(textMessage);

      BotApiResponse response = lineMessagingClient.broadcast(broadcast).get();
      log.info("Sent messages: {}", response);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
  /**
   * send notify message to chatroom
   */
  @GetMapping("/message")
  public void messageNotify() {
    try {

      PushMessage pushMessage =
          new PushMessage(roomId, new TextMessage("HelloController message reply for room id 回覆"));
      BotApiResponse response = lineMessagingClient.pushMessage(pushMessage).get();
      log.info("Sent messages: {}", response);

    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
