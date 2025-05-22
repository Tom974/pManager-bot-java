package dev.tom974;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Timer;
import java.util.TimerTask;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public SmartInitializingSingleton importProcessor() {
        return this.discordApplication::run;
    }

    private final DiscordApplication discordApplication;

    public DiscordApplication getDiscordInstance() {
        return this.discordApplication;
    }

    public Main(DiscordApplication discordApplication) {
        this.discordApplication = discordApplication;

        DiscordApplication.setMainInstance(this);

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            public void run() {
                discordApplication.sendUnitsUpdate();
            }
        }, 0, 60*1000); // every minute
    }
}