package ml.codeboy.thebot.commands;

import com.github.codeboy.piston4j.api.Runtime;
import com.github.codeboy.piston4j.api.*;
import ml.codeboy.Util;
import ml.codeboy.thebot.Config;
import ml.codeboy.thebot.events.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExecuteCommand extends Command {
    public ExecuteCommand() {
        super("run", "runs code in the specified language");
        setGuildOnlyCommand(false);
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
            List<Message.Attachment> attachments = event.getMessageReceivedEvent().getMessage().getAttachments();

            List<CodeFile> codeFiles = new ArrayList<>();
            String code = "";
            if (content.length() > language.length()) {
                code = content.substring(language.length() + 1);
                codeFiles.add(new CodeFile(code));

            }
            for (Message.Attachment attachment : attachments) {
                try {
                    String fileContent = Util.readUrl(attachment.getUrl());
                    String fileName = attachment.getFileName();
                    CodeFile codeFile = new CodeFile(fileName, fileContent);
                    codeFiles.add(codeFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            run(event, language, code, codeFiles);
        } else {
            SlashCommandInteractionEvent e = event.getSlashCommandEvent();
            run(event, e.getOption("language").getAsString(), e.getOption("code").getAsString(), Collections.emptyList());
        }
    }

    private void run(CommandEvent event, String language, String code, List<CodeFile> codeFiles) {
        //Initialise runtime
        Runtime r = Piston.getDefaultApi().getRuntimeUnsafe(language);
        if (r == null) {
            event.replyError("Language not found");
        } else {
            //ExecutionResults
            ExecutionRequest request = new ExecutionRequest(r.getLanguage(), r.getVersion(), codeFiles.toArray(new CodeFile[0]));
            ExecutionResult result = Piston.getDefaultApi().execute(request);
            if (code.length() == 0 && !codeFiles.isEmpty())
                code = codeFiles.get(0).getContent();
            sendResult(event, result, code);
        }
    }

    private void sendResult(CommandEvent event, ExecutionResult result, String code) {
        ExecutionOutput output = result.getOutput();
        ExecutionOutput error = result.getCompileOutput();
        //Message builder
        EmbedBuilder input = new EmbedBuilder();
        EmbedBuilder out = new EmbedBuilder();
        EmbedBuilder err = new EmbedBuilder();
        //Messages
        MessageEmbed[] ret;
        //Strings
        String errValue;
        String codeValue;
        //input builder
        input.setTitle("Execution output").setDescription("Language: " + result.getLanguage());
        codeValue = "```" + result.getLanguage() + "\n" + code + "\n```";
        boolean codeFits = true;
        if (codeValue.length() > 1024) {
            codeValue = "Code too long to fit in this message :(";
            codeFits = false;
        }
        input.addField("code", codeValue, false);
        //out builder
        out.addField("output", output.getOutput(), false);
        //err builder
        err.setTitle("Error output");
        if (error != null && error.getCode() != 0) {
            err.addField("Error:", "```bash\n" + error.getStderr() + "\n```", false);
            ret = new MessageEmbed[]{input.build(), out.build(), err.build()};
        } else {
            ret = new MessageEmbed[]{input.build(), out.build()};
        }
        if (event.isMessageEvent() && !codeFits) {
            event.getChannel().sendMessageEmbeds(Arrays.asList(ret)).addFile(code.getBytes(), "code.txt").queue();
        } else
            event.reply(ret);
    }
}
