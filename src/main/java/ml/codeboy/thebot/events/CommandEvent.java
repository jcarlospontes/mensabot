package ml.codeboy.thebot.events;

import ml.codeboy.thebot.Config;
import ml.codeboy.thebot.commands.sound.GuildMusicManager;
import ml.codeboy.thebot.commands.sound.PlayerManager;
import ml.codeboy.thebot.data.GuildData;
import ml.codeboy.thebot.data.GuildManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;
import java.util.ArrayList;

public abstract class CommandEvent {
    private final Event jdaEvent;
    private boolean ephermal = false;

    public CommandEvent(Event jdaEvent) {
        this.jdaEvent = jdaEvent;
    }

    public abstract void reply(String message);

    public abstract void reply(MessageEmbed... embed);

    public abstract User getUser();

    public abstract Member getMember();

    public void reply(EmbedBuilder builder) {
        reply(builder.build());
    }

    public void replyError(String error) {
        reply(new EmbedBuilder().setTitle("Error").setDescription(error).setColor(Color.RED).build());
    }

    //region getter
    public MessageReceivedEvent getMessageReceivedEvent() {
        return (MessageReceivedEvent) jdaEvent;
    }

    public SlashCommandInteractionEvent getSlashCommandEvent() {
        return (SlashCommandInteractionEvent) jdaEvent;
    }

    public String[] getArgs() {// TODO: Add better way to use arguments. Maybe something similar to JDAs slash commands
        if (isMessageEvent()) {
            String content = getMessageReceivedEvent().getMessage().getContentRaw();
            content = content.replaceFirst(Config.getInstance().prefix, "");

            String[] splitContent = content.split(" ", 2);

            if (splitContent.length == 1)
                return new String[0];

            String withoutCommand = splitContent[1];
            return withoutCommand.split(" ");
        } else {
            ArrayList<String> arguments = new ArrayList<>();
            for (OptionMapping o : getSlashCommandEvent().getOptions()) {
                arguments.add(o.getAsString());
            }
            return arguments.toArray(new String[0]);
        }
    }

    public Event getJdaEvent() {
        return jdaEvent;
    }

    public boolean isMessageEvent() {
        return this instanceof MessageCommandEvent;
    }

    public boolean isSlashCommandEvent() {
        return this instanceof SlashCommandCommandEvent;
    }

    public abstract Guild getGuild();

    public GuildData getGuildData() {
        return GuildManager.getInstance().getData(getGuild());
    }

    public abstract MessageChannel getChannel();

    public Message send(String message) {
        return getChannel().sendMessage(message).complete();
    }

    public Message send(MessageEmbed message) {
        return getChannel().sendMessageEmbeds(message).complete();
    }

    public Message send(EmbedBuilder builder) {
        return send(builder.build());
    }

    public void replyErrorUnknown() {
        replyError("unknown error");
    }

    public EmbedBuilder getBuilder() {
        return new EmbedBuilder();
    }

    public GuildMusicManager getManager() {
        return PlayerManager.getInstance().getMusicManager(getGuild());
    }

    public boolean isEphermal() {
        return ephermal;
    }

    public void setEphermal(boolean ephermal) {
        this.ephermal = ephermal;
    }

    //endregion
}
