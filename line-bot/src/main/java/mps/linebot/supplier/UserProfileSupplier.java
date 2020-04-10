package mps.linebot.supplier;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.profile.UserProfileResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class UserProfileSupplier {

  Event event;

  UserProfileResponse userProfile = null;
  LineMessagingClient lineMessagingClient;
  public UserProfileSupplier(LineMessagingClient lineMessagingClient, Event event ){
      this.lineMessagingClient = lineMessagingClient;
      this.event = event;
  }

  private void parse() throws ExecutionException, InterruptedException {
    CompletableFuture<UserProfileResponse> userProfileFuture =
        lineMessagingClient.getProfile(event.getSource().getUserId());
    userProfile = userProfileFuture.get();
  }

  public String getDisplayName() throws ExecutionException, InterruptedException {
    parse();
    return userProfile.getDisplayName();
  }
}
