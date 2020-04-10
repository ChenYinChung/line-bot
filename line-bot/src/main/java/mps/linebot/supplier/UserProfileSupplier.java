package mps.linebot.supplier;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.profile.UserProfileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class UserProfileSupplier {
  @Autowired private LineMessagingClient lineMessagingClient;

  Event event;
  UserProfileResponse userProfile = null;

  public UserProfileSupplier(Event event ){
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
