package dev.tom974;

import dev.tom974.models.*;
import jakarta.websocket.OnMessage;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateAvatarEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

@Component
public class DiscordApplication extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(DiscordApplication.class);
    private static JDA client;
    private static final HashMap<String, Collection<String>> voiceUnits = new HashMap<>();
    private final String[] trackedVoiceCategories = {
        "1109130331128348793", // NEVENTAKEN PORTOS
        "876808616458469430", // POLITIE PORTOS
        "876842710370238504", // POLITIE & MARECHAUSSEE
        "876855788495335434", // KMAR PORTOS
    };

    private static final Map<String, Unit> serviceUnits = new HashMap<>();
    private static final Map<Integer, Unit> leaders = new HashMap<>();

    private static final HashMap<String, String> trackedVoiceChannels = new HashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

//    public Map<String, Unit> getServiceUnits() {
////        Map<String, Unit> returnMap = new HashMap<>();
//
////        returnMap.put("channel", DiscordApplication.getVoiceUnits());
////        returnMap.put("users", DiscordApplication.getTrackedVoiceChannels());
//        return returnMap;
//    }

    public static Main main;


    public static void addAsLeader(String userId, Integer leaderType) {

    }

    // on avatar update


    public static void addToUnit(String userId, String unitId) {
        // check if unit exists
        if (!serviceUnits.containsKey(unitId)) {
            Unit un = new Unit();
            un.label = unitId;
            un.users = new ArrayList<>(Collections.singletonList(userId));
            serviceUnits.put(unitId, un);
        } else {
            Unit un = serviceUnits.get(unitId);
            un.users.add(userId);
        }

    }

    public static Map<String, Unit> getServiceUnits() {
        return serviceUnits;
    }

    public static boolean removeFromUnit(String userId, String unitId) {
        if (serviceUnits.containsKey(unitId)) {
            Unit un = serviceUnits.get(unitId);
            un.users.remove(userId);
        }

        return true;
    }

    public static boolean removeUnit(String unitId) {
        serviceUnits.remove(unitId);
        return true;
    }

    public static boolean changeUnitLabel(String unitId, String newLabel) {
        if (serviceUnits.containsKey(unitId)) {
            Unit un = serviceUnits.get(unitId);
            un.label = newLabel;
        }

        return true;
    }

    public static boolean changeUnitChannel(String unitId, String newChannel) {
        if (serviceUnits.containsKey(unitId)) {
            Unit un = serviceUnits.get(unitId);
            un.channel = newChannel;
        }

        return true;
    }

    public static boolean changeUnitUsers(String unitId, Collection<String> newUsers) {
        if (serviceUnits.containsKey(unitId)) {
            Unit un = serviceUnits.get(unitId);
            un.users = newUsers;
        }

        return true;
    }

    public Unit getUnitById(String unitId) {
        return serviceUnits.get(unitId);
    }

    @Autowired
    public DiscordApplication(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

//    public DiscordApplication(SimpMessagingTemplate template) {
//        this.messagingTemplate = template;
//    }

    public void run() {
        client = JDABuilder.createDefault("<token>")
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .enableIntents(
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MODERATION
            )
            .addEventListeners(this)
            .setActivity(Activity.customStatus("Lorenzo is een daggoe"))
            .setStatus(OnlineStatus.ONLINE)
            .enableCache(CacheFlag.VOICE_STATE)
            .build();
    }

    public static HashMap<String, String> getTrackedVoiceChannels() {
        return trackedVoiceChannels;
    }

    public JDA getClient() {
        return client;
    }

    public Unit getUnitByUser(String userId) {
        for (Unit unit : serviceUnits.values()) {
            if (unit.users.contains(userId)) {
                return unit;
            }
        }

        return null;
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        List<CommandData> commandData = new ArrayList<>();
        // clear old commands in guild
        event.getGuild().retrieveCommands().queue(commands -> {
            for (Command command : commands) {
                event.getGuild().deleteCommandById(command.getId()).queue();
            }
        });

        commandData.add(Commands.slash("check-role", "Check hoeveel mensen een training willen volgen")
            .addOption(ROLE, "role", "Selecteer rol om te checken")
            .setGuildOnly(true)
        );
        event.getGuild().updateCommands().addCommands(commandData).queue();

        if (event.getGuild().getId().equals("876808615749644338")) {
            for (String trackedVoiceCategory : trackedVoiceCategories) {

                event.getGuild().getVoiceChannels().forEach(voiceChannel -> {
                    if (Objects.equals(voiceChannel.getParentCategoryId(), trackedVoiceCategory)) {
                        trackedVoiceChannels.put(voiceChannel.getId(), voiceChannel.getName());
                    }
                });
            }
        }
    }

    public static HashMap<String, Collection<String>> getVoiceUnits() {
        return voiceUnits;
    }

    public void requestLogin(LoginRequest request) {
        this.messagingTemplate.convertAndSend("/porto/receiveMessage", request);
    }

    public static void setMainInstance(Main mainInst) {
        main = mainInst;
    }


    public void sendChannelSwitchUpdate(Member member, String oldChannelId, String newChannelId) {
        // Sends a message to a specific user/topic with the channel switch update
//        messagingTemplate.convertAndSendToUser(userId, "/queue/channel-switch", newChannelId);
        PortoChannelUpdate update = new PortoChannelUpdate();
        update.sender = "server";
        update.content = "User " + member.getId() + " switched to channel " + newChannelId;
        update.oldChannel = oldChannelId;
        update.newChannel = newChannelId;
        update.user = member.getId();
        update.nickname = member.getNickname();
        update.type = "channel_switch";
        update.unit = getUnitByUser(member.getId());
        this.messagingTemplate.convertAndSend("/porto/receiveMessage", update);
    }

    public void sendUnitsUpdate() {
        PortoUpdate pUpdate = new PortoUpdate();
//        pUpdate.units = serviceUnits;
        pUpdate.type = "portoUpdate";
        // get units from serviceUnits but without the key
        pUpdate.units = new ArrayList<>(serviceUnits.values());
        this.messagingTemplate.convertAndSend("/porto/receiveMessage", pUpdate);
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        // Check if the user is switching channels
        // get category channel is in
        String channelLeftId = "";
        String channelJoinedId = "";

        if (event.getChannelLeft() != null) channelLeftId = event.getChannelLeft().getId();
        if (event.getChannelJoined() != null) channelJoinedId = event.getChannelJoined().getId();
        Unit unit = getUnitByUser(event.getMember().getId());

        if (unit != null) {
            if (event.getChannelJoined() != null) {
                unit.channel = event.getChannelJoined().getId();
            }
        }

        sendChannelSwitchUpdate(event.getMember(), channelLeftId, channelJoinedId);

        if (event.getChannelJoined() != null) {
            // send update to websocket

        }

        if (event.getChannelJoined() != null) {


            String categoryId = event.getChannelJoined().getParentCategoryId();
            if (categoryId != null && Arrays.asList(trackedVoiceCategories).contains(categoryId)) {
                // send update to websocket
                if (!voiceUnits.containsKey(event.getChannelJoined().getId())) {
                    // create new entry with String[] containing userId and categoryId
//                voiceUnits.put(event.getChannelJoined().getId(), new String[] {event.getMember().getId()});
                    // create new entry with collection consisting of userId
                    voiceUnits.put(event.getChannelJoined().getId(), new ArrayList<>(Collections.singletonList(event.getMember().getId())));
                } else {
                    // add userId to existing entry
                    voiceUnits.get(event.getChannelJoined().getId()).add(event.getMember().getId());
                }
            }


//            voiceUnits.put(event.getChannelJoined().getId(), event.getMember().getId());
        }


        if (event.getChannelLeft() != null) {
            String categoryId = event.getChannelLeft().getParentCategoryId();
            if (categoryId != null && Arrays.asList(trackedVoiceCategories).contains(categoryId)) {
                if (voiceUnits.containsKey(event.getChannelLeft().getId())) {
                    voiceUnits.get(event.getChannelLeft().getId()).remove(event.getMember().getId());
                }
            }
//            voiceUnits.remove(event.getChannelLeft().getId());

        }


//        ApiController.SendUpdateToClients(channelLeftId, channelJoinedId, event.getMember().getId());


        // send update to websocket clients


//        if (event.getChannelJoined() != null && event.getChannelLeft() != null) {
//            // User switched channels
//            String userId = event.getMember().getUser().getAsTag();
//            String oldChannel = event.getChannelLeft().getName();
//            String newChannel = event.getChannelJoined().getName();
//
//            System.out.println(userId + " switched from " + oldChannel + " to " + newChannel);
//        } else if (event.getChannelJoined() != null) {
//            // User joined a new channel
//            String userId = event.getMember().getUser().getAsTag();
//            String newChannel = event.getChannelJoined().getName();
//
//            System.out.println(userId + " joined " + newChannel);
//        } else if (event.getChannelLeft() != null) {
//            // User left a channel
//            String userId = event.getMember().getUser().getAsTag();
//            String oldChannel = event.getChannelLeft().getName();
//
//            System.out.println(userId + " left " + oldChannel);
//        }
    }



    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent msgEvent) {
        if (msgEvent.getAuthor().isBot())
            return;

//        if (msgEvent.get)
        // check if bot is tagged by 625324951368499202
        if (msgEvent.getMessage().getContentDisplay().contains("@pManager")) {
            if (msgEvent.getAuthor().getId().equals("625324951368499202") || msgEvent.getAuthor().getId().equals("399624756023787530"))  {
//            msgEvent.getChannel().sendMessage("Hallo!").queue();
                // reply to message
                msgEvent.getMessage().reply("Ja baas wat is er?").queue();
            } else if (msgEvent.getAuthor().getId().equals("495971197373382658")) {
                msgEvent.getMessage().reply("woef").queue();
            } else {
                msgEvent.getMessage().reply("ik ben je daggoe niet").queue();
            }
            
            return;
        }


        System.out.println(msgEvent.getMessage().getContentDisplay());

        final String[] irriteerKerrieArray = {
            "ik haat",
            "maagd",
            "kut joch",
            "hou je snavel",
            "sukkel",
            "klootzak",
            "flikker",
            "domme doos",
            "je stinkt",
            "de lul",
            "flinke tik geven",
            "kut bot",
            "fuck jou",
            "kut dev"
        };

        final String[] replyKerrieArray = {
            "Doe nou eens lief Kerrie",
            "Doe nou eens lief Kerrie",
            "Doe nou eens lief Kerrie",
            "Normaal doen nu Kerrie",
            "Normaal doen nu Kerrie",
            "Normaal doen nu Kerrie",
            "Gedraag je Kerrie!!!!!!",
            "Zit je weer in week 4 Kerrie?",
        };

        if (msgEvent.getAuthor().getId().equals("528694507294687242") && Arrays.stream(irriteerKerrieArray).anyMatch(msgEvent.getMessage().getContentDisplay()::contains)) {
            Random random = new Random();
            int index = random.nextInt(replyKerrieArray.length);
//            System.out.println(replyKerrieArray[index]);
            // reply on message
//            msgEvent.getChannel().sendMessage(replyKerrieArray[index]).queue();
            msgEvent.getMessage().reply(replyKerrieArray[index]).queue();
        }

//        log.info("{} - {}: {}", msgEvent.getGuildChannel().getName(), msgEvent.getAuthor().getAsTag(), msgEvent.getMessage().getContentDisplay());
    }

    @OnMessage
    public void onVoiceStateUpdate(MessageReceivedEvent event) {
        System.out.println("Voice state update");
    }

    public static ArrayList<Long> removeAllRoles(Long guildId, Long userId) {
        ArrayList<Long> removedRoles = new ArrayList<>();
        Guild guild = client.getGuildById(guildId);
        assert guild != null;
        Objects.requireNonNull(guild.getMemberById(userId)).getRoles().forEach(role -> {
            removedRoles.add(role.getIdLong());
            guild.removeRoleFromMember(UserSnowflake.fromId(userId), role).queue();
        });

        return removedRoles;
    }

    public static void FireUser(FireUserRequest request) {
        for (String guildId : request.guildIds) {
            Guild guild = client.getGuildById(guildId);
            assert guild != null;
            try {
                guild.kick(UserSnowflake.fromId(request.userId)).queue();
            } catch (Exception e) {
                log.error("e: ", e);
                return;
            }
        }
    }

    public static boolean CheckForCLS(String userId, String guildId) {
        Guild guild = client.getGuildById(guildId);
        assert guild != null;
        AtomicBoolean activeAsCLS = new AtomicBoolean(false);
        guild.loadMembers().onSuccess(members -> {
            Member member = guild.getMemberById(userId);
            assert member != null;
            // get user's current nickname
            String nickname = member.getEffectiveName();
            activeAsCLS.set(nickname.contains("CLS"));

            // check if nickname contains "CLS"
        });

        return activeAsCLS.get();
    }

    public static void MoveUser(MoveUserRequest request) {
        Guild guild = client.getGuildById(request.guildId);

        assert guild != null;

        guild.loadMembers().onSuccess(members -> {
            for (String userId : request.userIds) {
                // move user
                Member member = guild.getMemberById(userId);
                assert member != null;

                guild.moveVoiceMember(member, guild.getVoiceChannelById(request.voiceChannelId)).queue();
            }
        });
    }

    public static boolean ChangeNickname(ChangeNicknameRequest request) {
        Guild guild = client.getGuildById(request.guildId);
        assert guild != null;
        try {
            Member member = guild.retrieveMember(UserSnowflake.fromId(request.userId)).complete();
            assert member != null;
            member.modifyNickname(request.newName).queue(_ -> {
                System.out.println("Nickname changed successfully");
            });
        } catch (Exception e) {
            log.error("e: ", e);
            return false;
        }

        return true;
    }

    public static String GenerateNickname(String callsign, String rankIcon, String firstname, String lastname, String service) {
        String nickname = "";
        if (service.equals("kmar")) {
            nickname = "[" + callsign + " " + rankIcon + "] " +  firstname + " " + lastname + ".";
        } else {
            nickname = "[ " + callsign + " - " + rankIcon + " ] " +  firstname + " " + lastname + ".";
        }

        return nickname;
    }

    public static boolean SwitchNickname(SwitchNicknameRequest request) {
        Guild guild = client.getGuildById(request.guildId);
        assert guild != null;
        try {
            Member member = guild.retrieveMember(UserSnowflake.fromId(request.userId)).complete();
            assert member != null;
            String currentNickname = member.getEffectiveName();
            // check if nickname is the dsi name or the normal name
            if (currentNickname.contains(request.ancillaryCallsign)) {
                // update with nhName
                member.modifyNickname(DiscordApplication.GenerateNickname(request.callsign, request.rankIcon, request.firstname, request.lastname, request.service)).queue(_ -> {
                    System.out.println("Nickname changed successfully");
                });
            } else {
                // update with dsiName
                member.modifyNickname(DiscordApplication.GenerateNickname(request.ancillaryCallsign, request.rankIcon, request.ancillaryFirstname, request.ancillaryLastname, request.service)).queue(_ -> {
                    System.out.println("Nickname changed successfully");
                });
            }
        } catch (Exception e) {
            log.error("e: ", e);
            return false;
        }

        return true;
    }

    public static boolean addBackRoles(Long guildId, Long userId, ArrayList<Long> roleIds) {
        Guild guild = client.getGuildById(guildId);
        assert guild != null;
        roleIds.forEach(roleId -> guild.addRoleToMember(UserSnowflake.fromId(userId), Objects.requireNonNull(guild.getRoleById(roleId))).queue());

        return true;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        // Only accept commands from guilds
        if (event.getGuild() == null)
            return;

        switch (event.getName())
        {
//            case "ban":
//                Member member = event.getOption("user").getAsMember(); // the "user" option is required, so it doesn't need a null-check here
//                User user = event.getOption("user").getAsUser();
//                ban(event, user, member);
//                break;
//            case "say":
//                say(event, Objects.requireNonNull(event.getOption("content")).getAsString()); // content is required so no null-check here
//                break;
            case "check-role":
                checkRole(event);
                break;
            default:
                event.reply("I can't handle that command right now :(").setEphemeral(true).queue();
        }
    }

    public void checkRole(SlashCommandInteractionEvent event) {
        Role role = Objects.requireNonNull(event.getOption("role")).getAsRole();
        // check if User has rol that contains words "TRAINER"
        User user = event.getUser();
        Guild guild = event.getGuild();
        assert guild != null;
        // update the cache

        Member member = event.getMember();
        assert member != null;
        boolean foundRequired = false;
        for (Role userRole : member.getRoles()) {
            if (userRole.getName().contains("TRAINER")) {
                foundRequired = true;
                break;
            }
        }

        if (!foundRequired) {
            event.reply("Je hebt niet genoeg permissies om dit command uit te voeren!").setEphemeral(true).queue();
            return;
        }

        guild.loadMembers().onSuccess(members -> {
            log.info("Members loaded: {}", members.size());


            // get members with roles
            // create embed
            EmbedBuilder embed = new EmbedBuilder();

    //        event.getGuild().getMembersWithRoles(role).forEach(memberWithRole -> embed.addField(memberWithRole.getUser().getAsTag(), memberWithRole.getAsMention(), false));

            embed.setTitle("Informatie - " + role.getName());
            embed.setDescription("Er zijn " + guild.getMembersWithRoles(role).size() + " leden die de training willen volgen");
            embed.setColor(0x00ff00); // green
            // list members with role as a field per member, use the username as the field name and then tag them below it
            guild.getMembersWithRoles(role).forEach(memberWithRole -> embed.addField(Objects.requireNonNull(memberWithRole.getUser().getName()), memberWithRole.getAsMention(), false));
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event)
    {
        String[] id = event.getComponentId().split(":"); // this is the custom id we specified in our button
        String authorId = id[0];
        String type = id[1];
        // Check that the button is for the user that clicked it, otherwise just ignore the event (let interaction fail)
        if (!authorId.equals(event.getUser().getId()))
            return;

        event.deferEdit().queue(); // acknowledge the button was clicked, otherwise the interaction will fail

        MessageChannel channel = event.getChannel();
        switch (type)
        {
            case "prune":
                int amount = Integer.parseInt(id[2]);
                event.getChannel().getIterableHistory()
                        .skipTo(event.getMessageIdLong())
                        .takeAsync(amount)
                        .thenAccept(channel::purgeMessages);
            case "delete":
                event.getHook().deleteOriginal().queue();
        }
    }

//    public void say(SlashCommandInteractionEvent event, String content)
//    {
//        event.reply(content).queue(); // This requires no permissions!
//    }
//
//    public void leave(SlashCommandInteractionEvent event)
//    {
//        if (!Objects.requireNonNull(event.getMember()).hasPermission(Permission.KICK_MEMBERS))
//            event.reply("You do not have permissions to kick me.").setEphemeral(true).queue();
//        else
//            event.reply("Leaving the server... :wave:") // Yep we received it
//                    .flatMap(_ -> Objects.requireNonNull(event.getGuild()).leave()) // Leave server after acknowledging the command
//                    .queue();
//    }


}
