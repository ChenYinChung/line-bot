package mps.linebot.betting;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Betting {

  public static enum BetEnum {
    庄("庄"),
    闲("闲"),
    和("和"),
    庄对("庄对"),
    闲龙宝("闲龙宝"),
    闲对("闲对"),
    大("大"),
    小("小"),
    任意对子("任意对子"),
    完美对子("任意对子"),
    超級六("超級六"),
    超級6("超級六");

    String name;
    BigDecimal amount;

    BetEnum(String name) {
      this.name = name;
      this.amount = BigDecimal.ZERO;
    }

    public String getName() {
      return this.name;
    }

    public void setAmount(String srcAmount) {
      this.amount = new BigDecimal(srcAmount);
    }

    public BigDecimal getAmount() {
      return this.amount;
    }
  }

  protected static String parseAmount(String src) {
    // 只讀純數字部份
    Pattern pattern = Pattern.compile("[\\d]{1,}");

    Matcher matcher = pattern.matcher(src);
    if (matcher.find()) {
      return matcher.group(0);
    }

    return null;
  }

  public static Optional<BetEnum> parseSrc(String src) {
    BetEnum[] bets = BetEnum.values();
    for (BetEnum betEnum : bets) {
      if (src.startsWith(betEnum.getName())) {
        String betAmount = parseAmount(src);
        // 符合投注，有金額內容
        if (betAmount != null) {
          betEnum.setAmount(betAmount);
          return Optional.of(betEnum);
        }
      }
    }

    return Optional.empty();
  }

  public static boolean isBettingString(String src){
    BetEnum[] bets = BetEnum.values();
    for (BetEnum betEnum : bets) {
      if (src.startsWith(betEnum.getName())) {
        return true;
      }
    }
    return false;
  }

  public static void main(String[] arg) {
    String test = "庄abc100.3元";

    Optional<BetEnum> optionalBetEnum = Betting.parseSrc(test);
    if (optionalBetEnum.isPresent()) {
      BetEnum betEnum = optionalBetEnum.get();

      log.info(betEnum.getName());
      log.info(""+betEnum.getAmount().intValue());
      String userName = "test1234";
      String response =
              String.format(
                      "第%d局 %s | %s%s | 投注成功",123,userName,betEnum.getName(),betEnum.getAmount().intValue());

      log.info(response);

    }


  }
}
