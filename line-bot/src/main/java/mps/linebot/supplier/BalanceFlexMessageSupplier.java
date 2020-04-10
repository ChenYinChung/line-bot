/*
 * Copyright 2018 LINE Corporation
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

package mps.linebot.supplier;

import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.flex.component.Box;
import com.linecorp.bot.model.message.flex.component.Icon;
import com.linecorp.bot.model.message.flex.component.Text;
import com.linecorp.bot.model.message.flex.component.Text.TextWeight;
import com.linecorp.bot.model.message.flex.container.Bubble;
import com.linecorp.bot.model.message.flex.unit.FlexFontSize;
import com.linecorp.bot.model.message.flex.unit.FlexLayout;
import com.linecorp.bot.model.message.flex.unit.FlexMarginSize;

import java.util.function.Supplier;

import static java.util.Arrays.asList;

public class BalanceFlexMessageSupplier implements Supplier<FlexMessage> {

  String userId;
  String balance;

  public BalanceFlexMessageSupplier(String userId, String balance) {
    this.userId = userId;
    this.balance = balance;
  }

  @Override
  public FlexMessage get() {

    final Box bodyBlock = createBodyBlock();
    final Bubble bubble = Bubble.builder().body(bodyBlock).build();

    return new FlexMessage(userId+" Balance Query", bubble);
  }

  private Box createBodyBlock() {
    final Text title =
        Text.builder()
            .text("Balance")
            .weight(TextWeight.BOLD)
            .size(FlexFontSize.XL)
            .build();

    final Box info = createInfoBox();

    return Box.builder().layout(FlexLayout.VERTICAL).contents(asList(title, info)).backgroundColor("#27ACB2").build();
  }

  private Box createInfoBox() {
    final Box place =
        Box.builder()
            .layout(FlexLayout.BASELINE)
            .spacing(FlexMarginSize.SM)
            .contents(
                asList(
                    Text.builder()
                        .text("User")
                        .color("#27ACF2")
                        .size(FlexFontSize.SM)
                        .flex(1)
                        .build(),
                    Text.builder()
                        .text(userId)
                        .wrap(true)
                        .color("#55ACF2")
                        .size(FlexFontSize.SM)
                        .flex(5)
                        .build()))
            .build();
    final Box time =
        Box.builder()
            .layout(FlexLayout.BASELINE)
            .spacing(FlexMarginSize.SM)
            .contents(
                asList(
                    Text.builder()
                        .text("Amount")
                        .color("#27ACF2")
                        .size(FlexFontSize.SM)
                        .flex(1)
                        .build(),
                    Text.builder()
                        .text(balance)
                        .wrap(true)
                        .color("#55ACF2")
                        .size(FlexFontSize.SM)
                        .flex(5)
                        .build()))
            .build();

    return Box.builder()
        .layout(FlexLayout.VERTICAL)
        .margin(FlexMarginSize.LG)
        .spacing(FlexMarginSize.SM)
        .contents(asList(place, time))
        .build();
  }
}
