package ml.codeboy.thebot.commands.leaderboard;

import ml.codeboy.thebot.commands.Command;
import ml.codeboy.thebot.data.UserData;
import ml.codeboy.thebot.data.UserDataManager;
import ml.codeboy.thebot.events.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TopCommand extends Command {
    private final LeaderBoard leaderBoard;
    private MessageEmbed lastTop;

    public TopCommand(LeaderBoard leaderBoard) {
        super(leaderBoard.getName() + "Top", leaderBoard.getCurrency() + " Bestenliste");
        setGuildOnlyCommand(false);
        this.leaderBoard = leaderBoard;
        lastTop = new EmbedBuilder().setTitle("Loading " + leaderBoard.getName() + "Top")
                .setDescription("please wait a few seconds\nI will update the message when I'm done").setColor(Color.RED).build();
    }


    @Override
    public void run(CommandEvent event) {
        event.reply(lastTop);
        new Thread(() -> update(event, false)).start();
    }

    private void update(CommandEvent event, boolean filter) {
        Guild guild = event.getGuild();
        JDA jda = guild.getJDA();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(leaderBoard.getName() + "Top");
        List<UserData> sorted = new ArrayList<>(UserDataManager.getInstance().getAllUserData());
        sorted.removeIf(d -> leaderBoard.getValue(d) == 0);
        sorted.sort(Comparator.comparingInt(d -> leaderBoard.getValue((UserData) d)).reversed());
        int limit = Math.min(10, sorted.size());
        int offset = 0;
        for (int i = 0; i < limit; i++) {
            if (i + offset >= sorted.size())
                break;
            UserData data = sorted.get(i + offset);
            try {
                User user = data.getUser(jda);
                if (filter && user.getMutualGuilds().isEmpty()) {
                    offset++;
                    i--;
                    continue;
                }
                builder.addField(i + 1 + ".", data.getTag(jda) + " " + leaderBoard.getValue(data), false);
            } catch (Exception e) {
                offset++;
                i--;
            }
        }
        event.edit(builder);
        if (!filter)
            lastTop = builder.build();
    }


}
