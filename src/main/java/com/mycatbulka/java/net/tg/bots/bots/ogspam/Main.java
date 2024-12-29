package com.mycatbulka.java.net.tg.bots.bots.ogspam;

import com.java.mycatbulka.libs.mylib.logging.Logging;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class Main {
    private static Properties properties;
    private static Bot bot;
    private static long chat_id = 0;
    public static void main(String[] args){
        Logging.info("Starting Bulka!");
        properties = new Properties();
        try {
            Logging.trace("Main", "Loading properties");
            properties.load(Files.newInputStream(Paths.get("bot.properties")));
            Logging.trace("Main", "Loaded file");
            if(properties.get("username") == null && properties.get("token") == null && properties.get("chat_id") == null)
                throw new IOException("properties file must contains username, token and chat_id");
            else
                chat_id = Long.parseLong((String) properties.get("chat_id"));
        } catch (Exception e){
            Logging.fatal("Main", "!!! CAN`T LOAD bot.properties!!! \nIT MUST BE IN THE ROOT DIR AND CONTAINS \nusername=r4ijefnsdvi \ntoken=3449895:jnjvdnwsjcbw \nchat_id=-123456789", e);
            System.exit(-1);
        }
        Logging.trace("Main", "Successful loaded properties file and checked values");

        Logging.trace("Main", "Starting bot");
        try {
            bot = new Bot(new DefaultBotOptions(), properties.getProperty("token"));
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            Logging.trace("Main", "Got botsAPI");
            botsApi.registerBot(bot);
            Logging.trace("Main", "Successful Registered bot");
        } catch (TelegramApiException e) {
            Logging.fatal("Main", "!!! CAN`T REGISTER BOT!!!", e);
            System.exit(-1);
        }


    }

    public static String getUserName(){
        return properties.get("username").toString();
    }

    public static Bot getBot() {
        return bot;
    }

    public static long getChat_id() {
        return chat_id;
    }
}