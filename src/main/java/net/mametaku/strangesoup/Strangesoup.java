package net.mametaku.strangesoup;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

import static org.bukkit.Material.BOWL;

public final class Strangesoup extends JavaPlugin implements Listener {

    // 設定パーミッション
    final String opPermission = "red.capsaicin.strangesoup.op";
    final String reloadPermission = "red.capsaicin.strangesoup.reload";
    final String allowPermission = "red.capsaicin.strangesoup.allowplayer";

    volatile boolean enableFlag;
    volatile List<String> disabledWorlds;
    volatile int flyTime;
    volatile int coolDownTime;

    HashMap<Player,Integer> playerTimeMap = new HashMap<>();
    HashMap<Player, BukkitTask> resetTimeMap = new HashMap();
    HashMap<UUID, Long> cooldowns = new HashMap();


    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        getCommand("strangesoup").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    void loadSettings(){
        reloadConfig();
        FileConfiguration config = getConfig();
        enableFlag = config.getBoolean("enableFlag");
        disabledWorlds = config.getStringList("disabledWorlds");
        flyTime = config.getInt("flyTime");
        coolDownTime = config.getInt("coolDownTime");
    }

    ItemStack stewItem(){
        ItemStack item = new ItemStack(Material.SUSPICIOUS_STEW, 1);
        ItemMeta itemmeta = item.getItemMeta();
        assert itemmeta != null;
        itemmeta.setCustomModelData(100);
        itemmeta.setDisplayName("§d§l飛ん汁");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("§l飛ぶほどうまいぞ！");
        itemmeta.setLore(lore);
        item.setItemMeta(itemmeta);
        return item;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player p = (Player) sender;
        if (!p.hasPermission(opPermission)) {
            p.sendMessage("§l[§5§lStrangeSoup§f§l]権限を持っていません。");
            return true;
        }
        if (args.length == 0){

            p.getInventory().addItem(stewItem());
            return true;
        }
        if (p.hasPermission(reloadPermission)) {
            if (args[0].equalsIgnoreCase("reload")) {
                loadSettings();
                p.sendMessage("§l[§5§lStrangeSoup§f§l]reloaded");
                return true;
            }
            return true;
        }
        return true;
    }

    @EventHandler
    public void FilledBowlFromMooshRoom(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        if (e.getRightClicked() instanceof MushroomCow && e.getPlayer().getInventory().getItemInMainHand().getType().equals(BOWL)) {
            if (((MushroomCow) e.getRightClicked()).getVariant().equals(MushroomCow.Variant.BROWN)) {
                int coolDown = coolDownTime; // Get number of seconds from wherever you want
                if (cooldowns.containsKey(p.getUniqueId())) {
                    long secondsLeft = ((cooldowns.get(p.getUniqueId()) / 1000) + coolDown) - (System.currentTimeMillis() / 1000);
                    if (secondsLeft > 0) {
                        e.setCancelled(true);
                        return;
                    }
                }
                Location loc = p.getLocation();
                loc.getWorld().playSound(loc, Sound.ENTITY_MOOSHROOM_SUSPICIOUS_MILK, 1f, 1f);
                p.getInventory().addItem(stewItem());
                ItemStack stack = p.getInventory().getItemInMainHand();
                stack.setAmount(stack.getAmount() - 1);
                p.getInventory().setItemInMainHand(stack);
                cooldowns.put(p.getUniqueId(), System.currentTimeMillis());
                e.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void EatSoup(PlayerItemConsumeEvent e){
        Player p = e.getPlayer();
        if(disabledWorlds.contains(p.getWorld().getName())){return;}
        if (Objects.requireNonNull(p.getInventory().getItemInMainHand().getItemMeta()).getCustomModelData() == 100){
            if (p.hasPermission(allowPermission)){
                Location loc = p.getLocation();
                loc.getWorld().playSound(loc, Sound.BLOCK_BREWING_STAND_BREW,1f,1f);
                playerTimeMap.put(p,flyTime);
                p.setAllowFlight(true);
                p.setFlying(true);
                flyTimer(p);
            }
        }
    }

    public void flyTimer(Player p){
        if (resetTimeMap.containsKey(p)){
            resetTimeMap.get(p).cancel();
        }
        resetTimeMap.put(p,Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                int time = playerTimeMap.get(p);
                if (time <= 15 && time != 0){
                    displayTime(p,time);
                }else if (time == 0){
                    displayTime(p,time);
                    p.setAllowFlight(false);
                    p.setFlying(false);
                    p.sendMessage("§f§lフライが終了しました。");
                    resetTimeMap.get(p).cancel();
                }
                playerTimeMap.put(p,playerTimeMap.get(p) - 1);
            }
        },0,20));
    }


    public void displayTime(Player p,int time){
        Location loc = p.getLocation();
        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL,1f,1f);
        TextComponent component = new TextComponent();
        component.setText("§4§lフライ終了まで残り"+time+"秒");
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR,component);
    }
}