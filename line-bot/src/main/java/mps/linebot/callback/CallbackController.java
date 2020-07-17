/*
 * Copyright 2016 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package mps.linebot.callback;

import com.google.common.io.ByteStreams;
import com.linecorp.bot.client.LineBlobClient;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.*;
import com.linecorp.bot.model.event.*;
import com.linecorp.bot.model.event.message.*;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.message.*;
import com.linecorp.bot.model.message.quickreply.QuickReply;
import com.linecorp.bot.model.message.quickreply.QuickReplyItem;
import com.linecorp.bot.model.message.template.*;
import com.linecorp.bot.model.profile.UserProfileResponse;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import mps.linebot.WebApplication;
import mps.linebot.betting.Betting;
import mps.linebot.supplier.BalanceFlexMessageSupplier;
import mps.linebot.supplier.MessageWithQuickReplySupplier;
import mps.linebot.supplier.UserProfileSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Slf4j
@LineMessageHandler
/**
 * 接收來自Line Server 的訊息
 * https://developers.line.biz/zh-hant/docs/messaging-api/building-bot/#webhook-event-types
 * <p>使用postman 測試，模擬Linse Server回應 , 只能使用post 1.Url : localhost:8080/callback 2.Headers
 * Content-Type : application/json Authorization : Bear+一個空白+Secret-Key -> Bear
 * Er1NFDxx8xZVh1BOEd2fo7v8HH1XWYCjGL9LUAP7nW+f8UfiAabD1Yu59Pr3mGTQioDuICCCP7rRHtObnz9iS4tBuoYXS1Nz/h+hSbayx9p3dyJaG338L52xOqyf0XMFoLwnmTEoCpxdsLgpDZ3GoQdB04t89/1O/w1cDnyilFU=
 * X-Line-Signature : 使用 Hmac sha-256 ,KeyPairs是Secret-key 加密body內容 ->
 * xpBBD8Pg2UkCxcI6k3Nh5JaO19qSGM4WEQgY+0Ht/Sg= 3.body: { "events": [ { "type": "message",
 * "replyToken": "18dd8eaf30c8404da1de06002cde05b9", "source": { "userId":
 * "U470a93ec2caaa5ab9a42f528884ff27b", "type": "user" }, "timestamp": 1497875299568, "message": {
 * "type": "text", "id": "6263732340746", "text": "遊戲" } } ] }
 */
public class CallbackController {
  @Autowired private LineMessagingClient lineMessagingClient;

  @Autowired private LineBlobClient lineBlobClient;

  /**
   * 接收文字
   * @param event
   * @throws Exception
   */
  @EventMapping
  public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
    TextMessageContent message = event.getMessage();

    handleTextContent(event.getReplyToken(), event, message);
  }

  /**
   * 表情圖案
   * @param event
   */
  @EventMapping
  public void handleStickerMessageEvent(MessageEvent<StickerMessageContent> event) {
    handleSticker(event.getReplyToken(), event.getMessage());
  }

  /**
   * 地理位置
   * @param event
   */
  @EventMapping
  public void handleLocationMessageEvent(MessageEvent<LocationMessageContent> event) {
    LocationMessageContent locationMessage = event.getMessage();
    reply(
        event.getReplyToken(),
        new LocationMessage(
            locationMessage.getTitle(),
            locationMessage.getAddress(),
            locationMessage.getLatitude(),
            locationMessage.getLongitude()));
  }

  /**
   * 接收圖檔
   * @param event
   * @throws IOException
   */
  @EventMapping
  public void handleImageMessageEvent(MessageEvent<ImageMessageContent> event) throws IOException {
    // You need to install ImageMagick
    handleHeavyContent(
        event.getReplyToken(),
        event.getMessage().getId(),
        responseBody -> {
          final ContentProvider provider = event.getMessage().getContentProvider();
          final DownloadedContent jpg;
          final DownloadedContent previewImg;
          if (provider.isExternal()) {
            jpg = new DownloadedContent(null, provider.getOriginalContentUrl());
            previewImg = new DownloadedContent(null, provider.getPreviewImageUrl());
          } else {
            jpg = saveContent("jpg", responseBody);
            previewImg = createTempFile("jpg");
            system("convert", "-resize", "240x", jpg.path.toString(), previewImg.path.toString());
          }
          reply(event.getReplyToken(), new ImageMessage(jpg.getUri(), previewImg.getUri()));
        });
  }

  /**
   * 音樂格式
   * @param event
   * @throws IOException
   */
  @EventMapping
  public void handleAudioMessageEvent(MessageEvent<AudioMessageContent> event) throws IOException {
    handleHeavyContent(
        event.getReplyToken(),
        event.getMessage().getId(),
        responseBody -> {
          final ContentProvider provider = event.getMessage().getContentProvider();
          final DownloadedContent mp4;
          if (provider.isExternal()) {
            mp4 = new DownloadedContent(null, provider.getOriginalContentUrl());
          } else {
            mp4 = saveContent("mp4", responseBody);
          }
          reply(event.getReplyToken(), new AudioMessage(mp4.getUri(), 100));
        });
  }

  /**
   * 影像格式
   * @param event
   * @throws IOException
   */
  @EventMapping
  public void handleVideoMessageEvent(MessageEvent<VideoMessageContent> event) throws IOException {
    // You need to install ffmpeg and ImageMagick.
    handleHeavyContent(
        event.getReplyToken(),
        event.getMessage().getId(),
        responseBody -> {
          final ContentProvider provider = event.getMessage().getContentProvider();
          final DownloadedContent mp4;
          final DownloadedContent previewImg;
          if (provider.isExternal()) {
            mp4 = new DownloadedContent(null, provider.getOriginalContentUrl());
            previewImg = new DownloadedContent(null, provider.getPreviewImageUrl());
          } else {
            mp4 = saveContent("mp4", responseBody);
            previewImg = createTempFile("jpg");
            system("convert", mp4.path + "[0]", previewImg.path.toString());
          }
          reply(event.getReplyToken(), new VideoMessage(mp4.getUri(), previewImg.uri));
        });
  }

  /**
   * 檔案
   * @param event
   */
  @EventMapping
  public void handleFileMessageEvent(MessageEvent<FileMessageContent> event) {
    this.reply(
        event.getReplyToken(),
        new TextMessage(
            String.format(
                "Received '%s'(%d bytes)",
                event.getMessage().getFileName(), event.getMessage().getFileSize())));
  }


  @EventMapping
  public void handleUnfollowEvent(UnfollowEvent event) {
    log.info("unfollowed this bot: {}", event);
  }

  @EventMapping
  public void handleFollowEvent(FollowEvent event) {
    String replyToken = event.getReplyToken();
    this.replyText(replyToken, "Got followed event");
  }

  /**
   * 小幫手被拉入群時，觸發的event event.getSource().getSenderId() SenderId 是Line群的ID, 可單對senderId push Data
   * save senderId to DB
   *
   * @param event
   */
  @EventMapping
  public void handleJoinEvent(JoinEvent event) {
    // TODO: call mps api add senderId for message push usual use.
    String replyToken = event.getReplyToken();
    this.replyText(replyToken, "Joined " + event.getSource());
  }

  @EventMapping
  public void handlePostbackEvent(PostbackEvent event) {
    String replyToken = event.getReplyToken();
    this.replyText(
        replyToken,
        "Got postback data "
            + event.getPostbackContent().getData()
            + ", param "
            + event.getPostbackContent().getParams().toString());
  }

  @EventMapping
  public void handleBeaconEvent(BeaconEvent event) {
    String replyToken = event.getReplyToken();
    this.replyText(replyToken, "Got beacon message " + event.getBeacon().getHwid());
  }

  @EventMapping
  public void handleMemberJoined(MemberJoinedEvent event) {
    String replyToken = event.getReplyToken();
    this.replyText(
        replyToken,
        "Got memberJoined message "
            + event.getJoined().getMembers().stream()
                .map(Source::getUserId)
                .collect(Collectors.joining(",")));
  }

  @EventMapping
  public void handleMemberLeft(MemberLeftEvent event) {
    log.info(
        "Got memberLeft message: {}",
        event.getLeft().getMembers().stream()
            .map(Source::getUserId)
            .collect(Collectors.joining(",")));
  }

  @EventMapping
  public void handleOtherEvent(Event event) {
    log.info("Received message(Ignored): {}", event);
  }

  private void reply(@NonNull String replyToken, @NonNull Message message) {
    reply(replyToken, singletonList(message));
  }

  private void reply(@NonNull String replyToken, @NonNull List<Message> messages) {
    reply(replyToken, messages, false);
  }

  private void reply(
      @NonNull String replyToken, @NonNull List<Message> messages, boolean notificationDisabled) {
    try {
      BotApiResponse apiResponse =
          lineMessagingClient
              .replyMessage(new ReplyMessage(replyToken, messages, notificationDisabled))
              .get();
      log.info("Sent messages: {}", apiResponse);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private void replyText(@NonNull String replyToken, @NonNull String message) {
    if (replyToken.isEmpty()) {
      throw new IllegalArgumentException("replyToken must not be empty");
    }
    if (message.length() > 1000) {
      message = message.substring(0, 1000 - 2) + "……";
    }
    this.reply(replyToken, new TextMessage(message));
  }

  private void handleHeavyContent(
      String replyToken, String messageId, Consumer<MessageContentResponse> messageConsumer) {
    final MessageContentResponse response;
    try {
      response = lineBlobClient.getMessageContent(messageId).get();
    } catch (InterruptedException | ExecutionException e) {
      reply(replyToken, new TextMessage("Cannot get image: " + e.getMessage()));
      throw new RuntimeException(e);
    }
    messageConsumer.accept(response);
  }

  private void handleSticker(String replyToken, StickerMessageContent content) {
    reply(replyToken, new StickerMessage(content.getPackageId(), content.getStickerId()));
  }

  /**
   * 主要文字操作反應區
   *
   * @param replyToken
   * @param event
   * @param content
   * @throws Exception
   */
  private void handleTextContent(String replyToken, Event event, TextMessageContent content)
      throws Exception {
    final String text = content.getText();
    String user = event.getSource().getUserId();
    log.info("Got text message from userId:{} replyToken:{}: text:{}",user, replyToken, text);
    String switchType = "";
    if (Betting.isBettingString(text)) {
      switchType = "下注";
    } else {
      switchType = text;
    }

    switch (switchType) {
      case "liff":
      {
        this.replyText(replyToken, "https://liff.line.me/1654461388-Z7R62D0z");
        break;
      }
      case "下注":
        {
          Optional<Betting.BetEnum> optionalBetEnum = Betting.parseSrc(text);
          Betting.BetEnum betEnum = optionalBetEnum.get();

          String senderId = event.getSource().getSenderId();
          String userId = event.getSource().getUserId();

          if (userId != null) {

            CompletableFuture<UserProfileResponse> userProfileFuture =
                lineMessagingClient.getProfile(event.getSource().getUserId());

            try {

              UserProfileSupplier userProfile = new UserProfileSupplier(lineMessagingClient, event);
              userProfile.showProfile();
              // TODO: Call MPS API betting here
              String current = "1234";
              String userName = userProfile.getDisplayName();
              String response =
                  String.format(
                      "第%s局 %s | %s%s | 投注成功",
                      current, userName, betEnum.getName(), betEnum.getAmount().intValue());
              this.replyText(replyToken, response);

            } catch (Exception e) {
              log.info(e.getMessage());
              this.replyText(replyToken, "** 下注格式有誤，請檢視下注格式 **");
            }
          } else {
            this.replyText(replyToken, "** 無法取得用戶資訊 **");
          }
          break;
        }
      case "注册":
      case "注冊":
        this.replyText(replyToken, "https://www.yabothai.com/signup");
        break;
      case "充值":
        {
          UserProfileSupplier userProfile = new UserProfileSupplier(lineMessagingClient, event);
          userProfile.showProfile();

          ConfirmTemplate confirmTemplate =
              new ConfirmTemplate(
                  "充值?", new MessageAction("Yes", "是"), new MessageAction("No", "否"));
          TemplateMessage templateMessage = new TemplateMessage("確認", confirmTemplate);
          this.reply(replyToken, templateMessage);
          break;
        }
      case "遊戲":
        {
          UserProfileSupplier userProfile = new UserProfileSupplier(lineMessagingClient, event);
          userProfile.showProfile();

          URI imageUrl = createUri("/static/buttons/logo.png");
          ButtonsTemplate buttonsTemplate =
              new ButtonsTemplate(
                  imageUrl,
                  "遊戲攻略",
                  "相關",
                  Arrays.asList(
                      new URIAction("進入遊戲", URI.create("https://www.yabothai.com/"), null),
                      new PostbackAction("餘額", "餘額", "餘額"),
                      new MessageAction("客服", "客服"),
                      new URIAction(
                          "常見問題", URI.create("https://www.w686.net/info/commonProblem"), null)));

          TemplateMessage templateMessage = new TemplateMessage("Game alter text", buttonsTemplate);
          this.reply(replyToken, templateMessage);
          break;
        }
      case "快速查詢":
        {
          UserProfileSupplier userProfile = new UserProfileSupplier(lineMessagingClient, event);
          userProfile.showProfile();

          this.reply(replyToken, new MessageWithQuickReplySupplier().get());
          break;
        }
      case "余额查询":
      case "余额":
        {
          UserProfileSupplier userProfile = new UserProfileSupplier(lineMessagingClient, event);
          userProfile.showProfile();

          String senderId = event.getSource().getSenderId();
          String userId = event.getSource().getUserId();
          // TODO : call api get user's balance
          this.reply(
              replyToken,
              new BalanceFlexMessageSupplier(userProfile.getDisplayName(), "123.456").get());
          break;
          }
      case "quick_reply":
        {
          UserProfileSupplier userProfile = new UserProfileSupplier(lineMessagingClient, event);
          userProfile.showProfile();

          final List<QuickReplyItem> items =
              Arrays.<QuickReplyItem>asList(
                  QuickReplyItem.builder()
                      .action(new MessageAction("MessageAction", "MessageAction"))
                      .build(),
                  QuickReplyItem.builder().action(CameraAction.withLabel("CameraAction")).build(),
                  QuickReplyItem.builder()
                      .action(CameraRollAction.withLabel("CemeraRollAction"))
                      .build(),
                  QuickReplyItem.builder().action(LocationAction.withLabel("Location")).build(),
                  QuickReplyItem.builder()
                      .action(
                          PostbackAction.builder()
                              .label("PostbackAction")
                              .text("PostbackAction clicked")
                              .data("{PostbackAction: true}")
                              .build())
                      .build());

          final QuickReply quickReply = QuickReply.items(items);

          TextMessage templateMessage =
              TextMessage.builder().text("快速查詢指令").quickReply(quickReply).build();

          this.reply(replyToken, templateMessage);

          break;
        }
      case "carousel":
        {
          UserProfileSupplier userProfile = new UserProfileSupplier(lineMessagingClient, event);
          userProfile.showProfile();

          URI imageUrl = createUri("/static/buttons/1040.jpg");
          CarouselTemplate carouselTemplate =
              new CarouselTemplate(
                  Arrays.asList(
                      new CarouselColumn(
                          imageUrl,
                          "hoge",
                          "fuga",
                          Arrays.asList(
                              new URIAction("Go to line.me", URI.create("https://line.me"), null),
                              new URIAction("Go to line.me", URI.create("https://line.me"), null),
                              new PostbackAction("Say hello1", "hello こんにちは"))),
                      new CarouselColumn(
                          imageUrl,
                          "hoge",
                          "fuga",
                          Arrays.asList(
                              new PostbackAction("言 hello2", "hello こんにちは", "hello こんにちは"),
                              new PostbackAction("言 hello2", "hello こんにちは", "hello こんにちは"),
                              new MessageAction("Say message", "Rice=米"))),
                      new CarouselColumn(
                          imageUrl,
                          "Datetime Picker",
                          "Please select a date, time or datetime",
                          Arrays.asList(
                              DatetimePickerAction.OfLocalDatetime.builder()
                                  .label("Datetime")
                                  .data("action=sel")
                                  .initial(LocalDateTime.parse("2017-06-18T06:15"))
                                  .min(LocalDateTime.parse("1900-01-01T00:00"))
                                  .max(LocalDateTime.parse("2100-12-31T23:59"))
                                  .build(),
                              DatetimePickerAction.OfLocalDate.builder()
                                  .label("Date")
                                  .data("action=sel&only=date")
                                  .initial(LocalDate.parse("2017-06-18"))
                                  .min(LocalDate.parse("1900-01-01"))
                                  .max(LocalDate.parse("2100-12-31"))
                                  .build(),
                              DatetimePickerAction.OfLocalTime.builder()
                                  .label("Time")
                                  .data("action=sel&only=time")
                                  .initial(LocalTime.parse("06:15"))
                                  .min(LocalTime.parse("00:00"))
                                  .max(LocalTime.parse("23:59"))
                                  .build()))));
          TemplateMessage templateMessage =
              new TemplateMessage("Carousel alt text", carouselTemplate);
          this.reply(replyToken, templateMessage);
          break;
        }
      case "image_carousel":
        {
          UserProfileSupplier userProfile = new UserProfileSupplier(lineMessagingClient, event);
          userProfile.showProfile();

          URI imageUrl = createUri("/static/buttons/1040.jpg");
          ImageCarouselTemplate imageCarouselTemplate =
              new ImageCarouselTemplate(
                  Arrays.asList(
                      new ImageCarouselColumn(
                          imageUrl,
                          new URIAction("Goto line.me", URI.create("https://line.me"), null)),
                      new ImageCarouselColumn(imageUrl, new MessageAction("Say message", "Rice=米")),
                      new ImageCarouselColumn(
                          imageUrl, new PostbackAction("言 hello2", "hello こんにちは", "hello こんにちは"))));
          TemplateMessage templateMessage =
              new TemplateMessage("ImageCarousel alt text", imageCarouselTemplate);
          this.reply(replyToken, templateMessage);
          break;
        }
      default:
        break;
    }
  }

  private static URI createUri(String path) {
    return ServletUriComponentsBuilder.fromCurrentContextPath()
        .scheme("https")
        .path(path)
        .build()
        .toUri();
  }

  private void system(String... args) {
    ProcessBuilder processBuilder = new ProcessBuilder(args);
    try {
      Process start = processBuilder.start();
      int i = start.waitFor();
      log.info("result: {} =>  {}", Arrays.toString(args), i);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      log.info("Interrupted", e);
      Thread.currentThread().interrupt();
    }
  }

  private static DownloadedContent saveContent(String ext, MessageContentResponse responseBody) {
    log.info("Got content-type: {}", responseBody);

    DownloadedContent tempFile = createTempFile(ext);
    try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
      ByteStreams.copy(responseBody.getStream(), outputStream);
      log.info("Saved {}: {}", ext, tempFile);
      return tempFile;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static DownloadedContent createTempFile(String ext) {
    String fileName = LocalDateTime.now().toString() + '-' + UUID.randomUUID() + '.' + ext;
    Path tempFile = WebApplication.downloadedContentDir.resolve(fileName);
    tempFile.toFile().deleteOnExit();
    return new DownloadedContent(tempFile, createUri("/downloaded/" + tempFile.getFileName()));
  }

  @Value
  private static class DownloadedContent {
    Path path;
    URI uri;
  }
}
