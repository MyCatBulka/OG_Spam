package com.mycatbulka.java.net.tg.bots.bots.ogspam;

import com.java.mycatbulka.libs.mylib.logging.Logging;
import com.java.mycatbulka.libs.mylib.utils.text.TextFormat;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Bot extends TelegramLongPollingBot {
    private long chat_id = 0;
    private String username = "";
    private List<String> lethal_triggers;
    private List<Long> admins;

    public Bot(DefaultBotOptions options, String botToken) {
        super(options, botToken);
        chat_id = Main.getChat_id();
        username = Main.getUserName();
        loadTriggers();
        saveTriggers();
        reloadAdmins();
    }

    public void loadTriggers(){
        Logging.trace("Bot", "Loading triggers");
        if(Files.exists(Paths.get("triggers.obj"))) {
            try {
                FileInputStream fis = new FileInputStream("triggers.obj");
                ObjectInputStream ois = new ObjectInputStream(fis);
                Object obj = ois.readObject();
                lethal_triggers = (List<String>) obj;
                Logging.trace("Bot", "Successful loaded triggers");
            } catch (Exception e) {
                Logging.error("Bot", "!Can`t load riggers.obj!", e);
                lethal_triggers = new ArrayList<>();
            }
        }
        else {
            lethal_triggers = new ArrayList<>();
            Logging.warn("Bot", "Can`t find triggers in triggers.obj, leave empty");
        }
    }

    public void saveTriggers(){
        Logging.trace("Bot", "Saving triggers");
        try {
            FileOutputStream fos = new FileOutputStream("triggers.obj");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(lethal_triggers);
            Logging.trace("Bot", "Successful saved triggers");
        } catch (Exception e) {
            Logging.error("Bot", "!Can`t save triggers.obj!", e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage()){
            if(update.getMessage().hasText()){
                Message message = update.getMessage();
                String text = message.getText();
                long in_chat_id = message.getChatId();
                if(chat_id == 0 || in_chat_id == chat_id){
                    if(!isAdmin(message.getFrom().getId())) {
                        for (String trigger : lethal_triggers) {
                            if (text.contains(trigger)) {
                                ban(message.getFrom().getId());
                                deleteMsg(message.getMessageId());
                            }
                        }
                    }

                    if(text.startsWith("/")){
                        String commandName = getCommandName(text);
                        if(commandName != null){
                            commandName = commandName.toLowerCase();
                            Logging.trace("Bot", TextFormat.format("Got command $ from $ $ @$ in $", commandName, message.getFrom().getFirstName(), message.getFrom().getLastName(), message.getFrom().getUserName(), message.getChat().getTitle()));
                            String arg;
                            String[] args = text.split(" ", 2);
                            if(args.length >= 2)
                                arg = args[1];
                            else
                                arg = "";
                            switch (commandName){
                                case "add_lethal_trigger":
                                    addLethalTrigger(message, arg);
                                    break;
                                case "rem_lethal_trigger":
                                    removeLethalTrigger(message, arg);
                                    break;
                                case "reload":
                                    reloadAdmins();
                                    sendMessage("Reloaded admins");
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }
    }

    public void addLethalTrigger(Message message, String arg){
        Logging.trace("Bot","Adding lethal trigger");
        User user = message.getFrom();
        boolean added = false;
        if(isAdmin(user.getId())){
            if(!arg.isEmpty()){
                if(lethal_triggers.contains(arg)) {
                    sendMessage("Trigger already added");
                }
                else {
                    lethal_triggers.add(arg);
                    added = true;
                }
            } else {
                Message replyToMessage = message.getReplyToMessage();
                if (replyToMessage != null){
                    if(lethal_triggers.contains(replyToMessage.getText())) {
                        sendMessage("Trigger already added");
                    }
                    else {
                        lethal_triggers.add(replyToMessage.getText());
                        added = true;
                    }
                    ban(replyToMessage.getFrom().getId());
                    deleteMsg(replyToMessage.getMessageId());
                } else {
                    sendMessage("You must write trigger in arguments (/add_lethal_trigger popa) or reply to message");
                }
            }
            if(added) {
                saveTriggers();
                sendMessage("Successful added trigger");
            } else {
                sendMessage("Can't added trigger");
            }
        } else {
            sendMessage("You don`t admin");
        }
    }

    public void removeLethalTrigger(Message message, String arg){
        Logging.trace("Bot","Removing lethal trigger");
        User user = message.getFrom();
        if(isAdmin(user.getId())){
            boolean removed = false;
            if(!arg.isEmpty()){
                removed = lethal_triggers.remove(arg);
            } else {
                Message replyToMessage = message.getReplyToMessage();
                if (replyToMessage != null){
                    removed = lethal_triggers.remove(replyToMessage.getText());
                } else {
                    sendMessage("You must write trigger in arguments (/remove_lethal_trigger popa) or reply to message");
                    return;
                }
            }
            if(removed) {
                saveTriggers();
                sendMessage("Successful removed trigger");
            } else {
                sendMessage("Can't remove trigger");
            }
        } else {
            sendMessage("You don`t admin");
        }
    }

    public void deleteMsg(int message_id){
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chat_id);
        deleteMessage.setMessageId(message_id);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            Logging.error("Bot", "Can`t delete message " + message_id, e);
        }
    }

    public void ban(long id) {
        BanChatMember banChatMember = new BanChatMember();
        banChatMember.setChatId(chat_id);
        banChatMember.setUserId(id);
        try {
            execute(banChatMember);
        } catch (TelegramApiException e) {
            Logging.error("Bot", "Can`t ban member " + id, e);
        }
    }
    public void reloadAdmins() {
        admins = getAdmins(chat_id);
    }

    public boolean isAdmin(long id){
        return admins.contains(id);
    }

    public void sendMessage(String text){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chat_id);
        sendMessage.setText(text);

        try {
            final int message_id = execute(sendMessage).getMessageId();
            Timer timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    deleteMsg(message_id);
                }
            };
            timer.schedule(timerTask, 60_000);
        } catch (TelegramApiException e) {
            Logging.error("Bot", "Can`t send message " + text, e);
        }
    }
    public List<Long> getAdmins(long chat) {
        List<ChatMember> chatMembers;
        List<Long> auth = new ArrayList<>();

        try {
            chatMembers = execute(new GetChatAdministrators(String.valueOf(chat)));
            for (ChatMember chatMember : chatMembers) {
                auth.add(chatMember.getUser().getId());
            }
        } catch (Exception e) {
            Logging.error("Bot", "Can`t get chat admins", e);
        }
        auth.add(1087968824L);

        return auth;
    }

    public String getCommandName(String command) {
        String[] parts = command.split(" ", 2);
        String commandName = parts[0];

        if (commandName.contains("@")) {
            String[] commandParts = commandName.split("@", 2);
            String commandUser = commandParts[1];

            if (!username.equals(commandUser)) {
                return null;
            } else {
                return commandParts[0].replace("/", "");
            }
        } else {
            return commandName.replace("/", "");
        }
    }

    @Override
    public String getBotUsername() {
        return username;
    }
}
