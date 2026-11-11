package com.example.pointsshop;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class DcraftPointsShop extends JavaPlugin implements CommandExecutor, Listener {

    private Object ppAPI = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // PlayerPoints Bağlantısı (Reflection)
        Plugin ppPlugin = getServer().getPluginManager().getPlugin("PlayerPoints");
        if (ppPlugin != null && ppPlugin.isEnabled()) {
            try { ppAPI = ppPlugin.getClass().getMethod("getAPI").invoke(ppPlugin); } catch (Exception ignored) {}
        }

        if (getCommand("puanmarket") != null) getCommand("puanmarket").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Dcraft Puan Marketi aktif!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        anaMenuAc(p);
        return true;
    }

    private void anaMenuAc(Player p) {
        int boyut = getConfig().getInt("menu-boyutu", 27);
        String baslik = "§8§l» §d§lDCRAFT PUAN MARKETİ §8§l«";
        Inventory inv = Bukkit.createInventory(null, boyut, baslik);
        
        ItemStack cam = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta cm = cam.getItemMeta(); if (cm != null) { cm.setDisplayName(" "); cam.setItemMeta(cm); }
        for (int i = 0; i < boyut; i++) inv.setItem(i, cam);

        ConfigurationSection urunler = getConfig().getConfigurationSection("urunler");
        if (urunler != null) {
            for (String key : urunler.getKeys(false)) {
                ConfigurationSection u = urunler.getConfigurationSection(key);
                if (u == null) continue;

                Material mat = Material.matchMaterial(u.getString("material", "STONE"));
                if (mat == null) mat = Material.STONE;

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(u.getString("isim", "§eÜrün"));
                    
                    List<String> lore = new ArrayList<>(u.getStringList("aciklama"));
                    lore.add("");
                    lore.add("§8§m-----------------------");
                    lore.add("§7Fiyat: §d" + u.getInt("fiyat") + " Dcraft Puanı");
                    lore.add("§8§m-----------------------");
                    lore.add("§aSatın almak için tıkla!");
                    lore.add("§8ID: " + key);
                    
                    meta.setLore(lore);
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(meta);
                }
                inv.setItem(u.getInt("slot", 0), item);
            }
        }

        // Bakiye Göstergesi
        ItemStack info = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta iMeta = info.getItemMeta();
        if (iMeta != null) {
            iMeta.setDisplayName("§e§lBakiyen");
            iMeta.setLore(Arrays.asList("§7Mevcut Dcraft Puanın: §d" + getPoints(p), "§7Harcamak için bir ürüne tıkla."));
            info.setItemMeta(iMeta);
        }
        inv.setItem(22, info);

        // Kapat
        ItemStack kapat = new ItemStack(Material.BARRIER);
        ItemMeta kMeta = kapat.getItemMeta();
        if (kMeta != null) { kMeta.setDisplayName("§c§lKapat"); kapat.setItemMeta(kMeta); }
        inv.setItem(26, kapat);

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f);
    }

    // Onay Menüsü
    private void onayMenuAc(Player p, String urunId) {
        ConfigurationSection u = getConfig().getConfigurationSection("urunler." + urunId);
        if (u == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, "§8§l» §6§lSATIN ALMA ONAYI §8§l«");
        
        ItemStack cam = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta cm = cam.getItemMeta(); if (cm != null) { cm.setDisplayName(" "); cam.setItemMeta(cm); }
        for (int i = 0; i < 27; i++) inv.setItem(i, cam);

        Material mat = Material.matchMaterial(u.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;
        
        ItemStack esya = new ItemStack(mat);
        ItemMeta eMeta = esya.getItemMeta();
        if (eMeta != null) {
            eMeta.setDisplayName(u.getString("isim"));
            eMeta.setLore(Arrays.asList("§7Ödenecek: §d" + u.getInt("fiyat") + " Puan"));
            esya.setItemMeta(eMeta);
        }
        inv.setItem(13, esya);

        ItemStack onay = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta oMeta = onay.getItemMeta();
        if (oMeta != null) {
            oMeta.setDisplayName("§a§lONAYLA");
            oMeta.setLore(Arrays.asList("§7Bu rütbeyi satın al.", "§8ID: " + urunId));
            oMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            oMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            onay.setItemMeta(oMeta);
        }
        inv.setItem(11, onay);

        ItemStack iptal = new ItemStack(Material.BARRIER);
        ItemMeta iMeta = iptal.getItemMeta();
        if (iMeta != null) {
            iMeta.setDisplayName("§c§lİPTAL");
            iptal.setItemMeta(iMeta);
        }
        inv.setItem(15, iptal);

        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        // ANA MENÜ
        if (title.equals("§8§l» §d§lDCRAFT PUAN MARKETİ §8§l«")) {
            e.setCancelled(true);
            if (clicked.getType() == Material.BARRIER) { p.closeInventory(); return; }
            
            if (clicked.hasItemMeta() && clicked.getItemMeta().getLore() != null) {
                for (String s : clicked.getItemMeta().getLore()) {
                    if (s.startsWith("§8ID: ")) {
                        String id = s.replace("§8ID: ", "");
                        onayMenuAc(p, id);
                        break;
                    }
                }
            }
            return;
        }

        // ONAY MENÜSÜ
        if (title.equals("§8§l» §6§lSATIN ALMA ONAYI §8§l«")) {
            e.setCancelled(true);
            if (clicked.getType() == Material.BARRIER) { anaMenuAc(p); return; }
            
            if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
                String id = null;
                if (clicked.getItemMeta().getLore() != null) {
                    for (String s : clicked.getItemMeta().getLore()) {
                        if (s.startsWith("§8ID: ")) { id = s.replace("§8ID: ", ""); break; }
                    }
                }
                if (id != null) satinAl(p, id);
            }
            return;
        }
    }

    private void satinAl(Player p, String urunId) {
        ConfigurationSection u = getConfig().getConfigurationSection("urunler." + urunId);
        if (u == null) return;

        int fiyat = u.getInt("fiyat");
        String komut = u.getString("komut", "").replace("%player%", p.getName());
        String isim = u.getString("isim", "Ürün");

        int bakiye = getPoints(p);
        if (bakiye < fiyat) {
            p.sendMessage("§cYetersiz Dcraft Puanın! Gerekli: §d" + fiyat);
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            p.closeInventory();
            return;
        }

        // Puanı Düş
        if (ppAPI != null) {
            try {
                ppAPI.getClass().getMethod("take", UUID.class, int.class).invoke(ppAPI, p.getUniqueId(), fiyat);
            } catch (Exception ex) {
                p.sendMessage("§cPuan düşülürken bir hata oluştu!");
                return;
            }
        }

        // Komutu Çalıştır (LuckPerms Rütbe Verme)
        if (!komut.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), komut);
        }

        // Efektler
        p.sendMessage("§a§l✔ §aBaşarıyla §e" + isim + " §arütbesini satın aldın!");
        p.sendMessage("§7Hesabından §d" + fiyat + " Puan §7düşüldü.");
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        p.sendTitle("§a§lSATIN ALINDI", "§7" + isim, 10, 50, 10);

        p.closeInventory();
    }

    private int getPoints(Player p) {
        if (ppAPI == null) return 0;
        try {
            return (int) ppAPI.getClass().getMethod("look", UUID.class).invoke(ppAPI, p.getUniqueId());
        } catch (Exception ex) {
            return 0;
        }
    }
                             }
