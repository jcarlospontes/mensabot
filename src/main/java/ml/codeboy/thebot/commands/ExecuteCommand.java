package ml.codeboy.thebot.commands;

import com.github.codeboy.piston4j.api.ExecutionOutput;
import com.github.codeboy.piston4j.api.ExecutionResult;
import com.github.codeboy.piston4j.api.Piston;
import com.github.codeboy.piston4j.api.Runtime;
import ml.codeboy.thebot.Config;
import ml.codeboy.thebot.events.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

public class ExecuteCommand extends Command {
    public ExecuteCommand() {
        super("run", "runs code in the specified language");
    }

    @Override
    public SlashCommandData getCommandData() {
        return super.getCommandData().addOption(OptionType.STRING, "language", "see available languages using the languages command", true, true)
                .addOption(OptionType.STRING, "code", "the code to run", true);
    }



    @Override
    public void autoComplete(String option, List<String> options) {
        switch (option) {
            case "language": {
                for (Runtime runtime : Piston.getDefaultApi().getRuntimes()) {
                    options.add(runtime.getLanguage());
                }
                break;
            }
        }
    }

    @Override
    public void run(CommandEvent event) {
        if (event.isMessageEvent()) {
            String language = event.getArgs()[0];
            String content = event.getMessageReceivedEvent().getMessage().getContentRaw();
            content = content.replaceFirst(Config.getInstance().prefix, "");
            content = content.split(" ", 2)[1];
            String code = content.substring(language.length() + 1);
            run(event, language, code);
        } else {
            SlashCommandInteractionEvent e = event.getSlashCommandEvent();
            run(event, e.getOption("language").getAsString(), e.getOption("code").getAsString());
        }
    }

    private void run(CommandEvent event, String language, String code) {
        Runtime r = Piston.getDefaultApi().getRuntimeUnsafe(language);
        if (r == null) {
            event.replyError("Language not found");
        } else {
            ExecutionResult result = r.execute(code);
            EmbedBuilder input = new EmbedBuilder();
            input.setTitle("Execution output").setDescription("Language: " + result.getLanguage());
            String codeValue="```" + language + "\n" + code + "\n```";
            if(codeValue.length()>1024)
                codeValue="Code too long to fit in this message :(";
            input.addField("code", codeValue, false);
            ExecutionOutput output = result.getOutput();

            EmbedBuilder out = new EmbedBuilder();
            System.out.println(output.getOutput().length());
            out.addField("output", output.getOutput(), false);
            event.reply(input.build(),out.build());
        }
    }
}
