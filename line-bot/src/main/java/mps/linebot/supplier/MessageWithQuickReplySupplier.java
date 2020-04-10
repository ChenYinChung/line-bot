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

import com.linecorp.bot.model.action.*;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.quickreply.QuickReply;
import com.linecorp.bot.model.message.quickreply.QuickReplyItem;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class MessageWithQuickReplySupplier implements Supplier<Message> {
  @Override
  public Message get() {

    QuickReplyItem betCLI =
        QuickReplyItem.builder()
            .action(
                new MessageAction(
                    "投注指令",
                    "您好, 投注指令格式为玩法＋金额, 例如庄100\n"
                        + "\n"
                        + "如果投注成功, 系统会回传讯息；如果投注不成功, 请确认投注格式是否有误, 或余额是否不足, 如有任何问题均可联系客服询问"))
            .build();
    QuickReplyItem awardCLI =
        QuickReplyItem.builder()
            .action(new MessageAction("打赏指令", "您好, 打赏指令格式为打赏＋金额, 例如打赏10"))
            .build();
    QuickReplyItem login =
        QuickReplyItem.builder()
            .action(new MessageAction("平台登錄", "开启 https://www.yabothai.com/"))
            .build();
    QuickReplyItem discount =
        QuickReplyItem.builder().action(new MessageAction("最新优惠", "机器人丢出优惠活动")).build();
    QuickReplyItem register =
        QuickReplyItem.builder()
            .action(new MessageAction("如何注册", "您好, 请点击「选单-会员注册」, 或联系客服询问"))
            .build();
    QuickReplyItem deposite =
        QuickReplyItem.builder()
            .action(new MessageAction("如何充值", "您好, 请点击「选单-会员充值」, 或联系客服询问"))
            .build();
    QuickReplyItem withdraw =
        QuickReplyItem.builder()
            .action(new MessageAction("如何提现", "您好, 提现请至娱乐城申请(https://yabothai.com),或联系客服询问"))
            .build();
    QuickReplyItem balance =
        QuickReplyItem.builder()
            .action(new MessageAction("如何查询余额", "您好, 请点击「选单-馀额查询」, 或联系客服询问"))
            .build();
    QuickReplyItem history =
        QuickReplyItem.builder()
            .action(new MessageAction("如何查询战绩", "您好, 请点击「选单-战绩查询」, 或联系客服询问"))
            .build();

    final List<QuickReplyItem> items =
        Arrays.<QuickReplyItem>asList(
            betCLI, awardCLI, login, discount, register, deposite, withdraw, balance, history);

    QuickReply quickReply = QuickReply.items(items);
    return TextMessage.builder().quickReply(quickReply).build();
  }
}
