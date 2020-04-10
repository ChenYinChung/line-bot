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
import org.springframework.web.bind.annotation.RequestParam;
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

//  String userId = "U15bfc8c8105dfa155b9f5ae32e4a31a0";
//  String roomId = "Ra1ba6323c881c0cba849a69ae42784fd";

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
  public void pushMessage(@RequestParam(defaultValue = "U15bfc8c8105dfa155b9f5ae32e4a31a0") String userId,@RequestParam(defaultValue = "單一發送") String content ) {
    try {

      if(content.equals("單一發送") && userId.equals("U15bfc8c8105dfa155b9f5ae32e4a31a0")){
        content = "您是不是忘了填userId & content , 試一下https://sammyline.herokuapp.com/push?userId=aaa&content=你想說什麼";
      }

      PushMessage pushMessage = new PushMessage(userId, new TextMessage(content));
      BotApiResponse response = lineMessagingClient.pushMessage(pushMessage).get();
      log.info("Sent messages: {}", response);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 針對指定的人員傳送
   */
  @GetMapping("/multicast")
  public void multicastMessage(@RequestParam(defaultValue = "U15bfc8c8105dfa155b9f5ae32e4a31a0") String userIds,@RequestParam(defaultValue = "可以多人傳送喔") String content) {
    try {

      if(content.equals("可以多人傳送喔") && userIds.equals("U15bfc8c8105dfa155b9f5ae32e4a31a0")){
        content = "您是不是忘了填userIds & content , 試一下https://sammyline.herokuapp.com/multicast?userIds=aaa,bbb,ccc&content=你想說什麼";
      }

      Set<String> ids = new HashSet<>();
      String[] uids = userIds.split(",");

      for (String id : uids) {
        ids.add(id);
      }
      Multicast multicast = new Multicast(ids, new TextMessage(content));

      BotApiResponse response = lineMessagingClient.multicast(multicast).get();
      log.info("Sent messages: {}", response);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 針對所有人
   */
  @GetMapping("/broadcast")
  public void broadcastMessage(@RequestParam(defaultValue = "針對所有人都會收到") String content) {
    try {
      if(content.equals("針對所有人都會收到")){
        content = "您是不是忘了填 content , 試一下https://sammyline.herokuapp.com/broadcast?content=大家都會收到訊息耶";
      }


      TextMessage textMessage = new TextMessage("HelloController broadcastMessage for all users 回覆");
      Broadcast broadcast = new Broadcast(textMessage);

      BotApiResponse response = lineMessagingClient.broadcast(broadcast).get();
      log.info("Sent messages: {}", response);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
  /**
   * 針對room ID 發送
   */
  @GetMapping("/message")
  public void messageNotify(@RequestParam(defaultValue = "Ra1ba6323c881c0cba849a69ae42784fd") String roomId,@RequestParam(defaultValue = "群發給Bot同群的人") String content ) {
    try {

      if(content.equals("群發給Bot同群的人") && roomId.equals("Ra1ba6323c881c0cba849a69ae42784fd")){
        content = "您是不是忘了填roomId & content , 試一下https://sammyline.herokuapp.com/message?roomId=aaa&content=你想對大家說什麼";
      }

      PushMessage pushMessage =
          new PushMessage(roomId, new TextMessage(content));
      BotApiResponse response = lineMessagingClient.pushMessage(pushMessage).get();
      log.info("Sent messages: {}", response);

    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
