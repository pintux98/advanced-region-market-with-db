package net.alex9849.arm.regions;

import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.minifeatures.AutoPrice;
import net.alex9849.arm.Messages;
import net.alex9849.arm.Permission;
import net.alex9849.arm.exceptions.InputException;
import net.alex9849.arm.minifeatures.PlayerRegionRelationship;
import net.alex9849.arm.minifeatures.teleporter.Teleporter;
import net.alex9849.inter.WGRegion;
import org.bukkit.*;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public abstract class Region {
    public static boolean completeTabRegions;

    protected WGRegion region;
    protected String regionworld;
    protected ArrayList<Sign> sellsign;
    protected ArrayList<Location> builtblocks;
    protected double price;
    protected boolean sold;
    protected boolean autoreset;
    protected boolean isHotel;
    protected long lastreset;
    protected static int resetcooldown;
    private static YamlConfiguration regionsconf;
    protected RegionKind regionKind;
    protected Location teleportLocation;
    protected boolean isDoBlockReset;
    protected List<Region> subregions;
    protected boolean isTown;

    public Region(WGRegion region, String regionworld, List<Sign> sellsign, double price, Boolean sold, Boolean autoreset,
                  Boolean isHotel, Boolean doBlockReset, RegionKind regionKind, Location teleportLoc, long lastreset, Boolean createSchematic, List<Region> subregions, boolean isTown){
        this.region = region;
        this.sellsign = new ArrayList<Sign>(sellsign);
        this.sold = sold;
        this.price = price;
        this.regionworld = regionworld;
        this.regionKind = regionKind;
        this.autoreset = autoreset;
        this.isDoBlockReset = doBlockReset;
        this.lastreset = lastreset;
        this.builtblocks = new ArrayList<Location>();
        this.isHotel = isHotel;
        this.teleportLocation = teleportLoc;
        this.subregions = new ArrayList<Region>();

        File pluginfolder = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket").getDataFolder();
        File builtblocksdic = new File(pluginfolder + "/schematics/" + this.regionworld + "/" + region.getId() + "--builtblocks.schematic");
        if(builtblocksdic.exists()){
            try {
                FileReader filereader = new FileReader(builtblocksdic);
                BufferedReader reader = new BufferedReader(filereader);
                String line;
                while ((line = reader.readLine()) != null){
                    String[] lines = line.split(";", 4);
                    int x = Integer.parseInt(lines[1]);
                    int y = Integer.parseInt(lines[2]);
                    int z = Integer.parseInt(lines[3]);
                    Location loc = new Location(Bukkit.getWorld(lines[0]), x, y, z);
                    builtblocks.add(loc);
                }
                reader.close();
                filereader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            this.builtblocks = new ArrayList<Location>();
        }
        if(createSchematic){
            this.createSchematic();
        }

    }

    public void setTeleportLocation(Location loc) {
        this.teleportLocation = loc;
        RegionManager.writeRegionsToConfig();
    }

    public void addSubRegion(Region region, Boolean save) {
        this.subregions.add(region);

        if(save) {
            RegionManager.writeRegionsToConfig();
        }
    }

    public void delete() {
        for(int i = 0; i < this.sellsign.size(); i++) {
            Location loc = this.sellsign.get(i).getLocation();
            loc.getBlock().setType(Material.AIR);
            this.removeSign(loc, null);
            i--;
        }
    }

    public void addBuiltBlock(Location loc) {
        this.builtblocks.add(loc);
        try {
            File pluginfolder = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket").getDataFolder();
            File builtblocksdic = new File(pluginfolder + "/schematics/" + this.regionworld + "/" + region.getId() + "--builtblocks.schematic");
            if(!builtblocksdic.exists()){
                File builtblocksfolder = new File(pluginfolder + "/schematics/" + this.regionworld);
                builtblocksfolder.mkdirs();
                builtblocksdic.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(builtblocksdic, true);
            BufferedWriter writer = new BufferedWriter(fileWriter);
            loc.getBlock().getType();
            writer.write(loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ());
            writer.newLine();
            writer.close();
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addSign(Location loc){
        Sign newsign = (Sign) loc.getBlock().getState();
        sellsign.add(newsign);
        RegionManager.writeRegionsToConfig();
        this.updateSignText(newsign);
        Bukkit.getServer().getWorld(regionworld).save();

    }

    public boolean removeSign(Location loc){
        return this.removeSign(loc, null);
    }

    public boolean removeSign(Location loc, Player destroyer){
        for(int i = 0; i < this.sellsign.size(); i++){
            if(this.sellsign.get(i).getWorld().getName().equals(loc.getWorld().getName())){
                if(this.sellsign.get(i).getLocation().distance(loc) == 0){
                    this.sellsign.remove(i);
                    if(destroyer != null) {
                        String message = Messages.SIGN_REMOVED_FROM_REGION.replace("%remaining%", this.getNumberOfSigns() + "");
                        destroyer.sendMessage(Messages.PREFIX + message);
                    }
                    if(this.sellsign.size() == 0) {
                        RegionManager.removeRegion(this);
                        if(destroyer != null) {
                            destroyer.sendMessage(Messages.PREFIX + Messages.REGION_REMOVED_FROM_ARM);
                        }
                    }
                    RegionManager.writeRegionsToConfig();

                    return true;
                }
            }
        }
        return false;
    }

    public WGRegion getRegion() {
        return region;
    }

    public void updateSigns() {
        for (int i = 0; i < this.sellsign.size(); i++) {
            Location loc = new Location(this.sellsign.get(i).getLocation().getWorld(), this.sellsign.get(i).getLocation().getBlockX(), this.sellsign.get(i).getLocation().getBlockY(), this.sellsign.get(i).getLocation().getBlockZ());
            if (loc.getBlock().getType() != this.sellsign.get(i).getType()) {
                loc.getBlock().setType(this.sellsign.get(i).getType());
                loc.getBlock().setBlockData(this.sellsign.get(i).getBlockData());
                this.sellsign.set(i, (Sign) loc.getBlock().getState());
            }

            this.updateSignText(this.sellsign.get(i));
        }
    }

    public String getRegionworld() {
        return regionworld;
    }

    public double getPrice(){
        return this.price;
    }

    public boolean hasSign(Sign sign){
        for(int i = 0; i < this.sellsign.size(); i++){
            if(this.sellsign.get(i).getWorld().getName().equalsIgnoreCase(sign.getWorld().getName())){
                if(this.sellsign.get(i).getLocation().distance(sign.getLocation()) == 0){
                    return true;
                }
            }
        }
        return false;
    }

    public int getNumberOfSigns(){
        return this.sellsign.size();
    }

    public static double calculatePrice(WGRegion region, String regiontype){

        Vector min = region.getMinPoint();
        Vector max = region.getMaxPoint();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();
        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        double price = 0;
        double priceperblock = 0;

        for(int i = 0; i < AutoPrice.getAutoPrices().size(); i++){
            if(AutoPrice.getAutoPrices().get(i).equals(regiontype)){
                priceperblock = AutoPrice.getAutoPrices().get(i).getPricePerSquareMeter();
            }
        }

        for(int x = minX; x <= maxX; x++){
            for(int z = minZ; z <= maxZ; z++) {
                if(region.contains(x, minY, z)){
                    price = price + priceperblock;
                }
            }
        }
        if(price == 0) {
            return Double.parseDouble(regiontype);
        } else {
            return price;
        }
    }

    public boolean setKind(String kind){
        RegionKind regionkind = RegionKind.getRegionKind(kind);

        if(regionkind == null) {
            return false;

        } else if(regionkind == RegionKind.DEFAULT) {
            this.regionKind = RegionKind.DEFAULT;
            RegionManager.writeRegionsToConfig();
            return true;

        } else {
            this.regionKind = regionkind;
            RegionManager.writeRegionsToConfig();
            return true;
        }
    }

    public RegionKind getRegionKind(){
        return this.regionKind;
    }

    public boolean isSold() {
        return sold;
    }

    public void createSchematic(){
        AdvancedRegionMarket.getWorldEditInterface().createSchematic(this.getRegion(), this.getRegionworld(), AdvancedRegionMarket.getWorldedit().getWorldEdit());
    }

    public boolean resetBlocks(){
        this.resetBuiltBlocks();
        AdvancedRegionMarket.getWorldEditInterface().resetBlocks(this.getRegion(), this.getRegionworld(), AdvancedRegionMarket.getWorldedit().getWorldEdit());

        return true;
    }

    public void resetBuiltBlocks() {
        File pluginfolder = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket").getDataFolder();
        File builtblocksdic = new File(pluginfolder + "/schematics/" + this.regionworld + "/" + region.getId() + "--builtblocks.schematic");
        if(builtblocksdic.exists()){
            builtblocksdic.delete();
            this.builtblocks = new ArrayList<>();
        }
    }

    public void regionInfo(CommandSender sender){
        String owners = "";
        String members = "";
        List<UUID> ownerslist = this.getRegion().getOwners();
        List<UUID> memberslist = this.getRegion().getMembers();
        for(int i = 0; i < ownerslist.size() - 1; i++){
            owners = owners + Bukkit.getOfflinePlayer(ownerslist.get(i)).getName() + ", ";
        }
        if(ownerslist.size() != 0){
            owners = owners + Bukkit.getOfflinePlayer(ownerslist.get(ownerslist.size() - 1)).getName();
        }
        for(int i = 0; i < memberslist.size() - 1; i++){
            members = members + Bukkit.getOfflinePlayer(memberslist.get(i)).getName() + ", ";
        }
        if(memberslist.size() != 0){
            members = members + Bukkit.getOfflinePlayer(memberslist.get(memberslist.size() - 1)).getName();
        }



        sender.sendMessage(Messages.REGION_INFO);
        sender.sendMessage(Messages.REGION_INFO_ID + this.getRegion().getId());
        sender.sendMessage(Messages.REGION_INFO_SOLD + Messages.convertYesNo(this.isSold()));
        sender.sendMessage(Messages.REGION_INFO_PRICE + this.price + " " + Messages.CURRENCY);
        sender.sendMessage(Messages.REGION_INFO_TYPE + this.getRegionKind().getDisplayName());
        sender.sendMessage(Messages.REGION_INFO_OWNER + owners);
        sender.sendMessage(Messages.REGION_INFO_MEMBERS + members);
        if(sender.hasPermission(Permission.ADMIN_INFO)){
            String autoresetmsg = Messages.REGION_INFO_AUTORESET + Messages.convertYesNo(this.autoreset);
            if((!AdvancedRegionMarket.getEnableAutoReset()) && this.autoreset){
                autoresetmsg = autoresetmsg + " (but globally disabled)";
            }
            sender.sendMessage(autoresetmsg);
            sender.sendMessage(Messages.REGION_INFO_HOTEL + Messages.convertYesNo(this.isHotel));
            sender.sendMessage(Messages.REGION_INFO_DO_BLOCK_RESET + Messages.convertYesNo(this.isDoBlockReset));
        }
        this.displayExtraInfo(sender);
    }

    public abstract void displayExtraInfo(CommandSender sender);

    public abstract void updateRegion();

    public boolean getAutoreset() {
        return autoreset;
    }

    public void setAutoreset(Boolean state){
        this.autoreset = state;
        RegionManager.writeRegionsToConfig();
    }

    public Material getLogo() {
        return this.getRegionKind().getMaterial();
    }

    public Location getTeleportLocation() {
        return this.teleportLocation;
    }

    public void setNewOwner(OfflinePlayer member){
        ArrayList<UUID> owner = this.getRegion().getOwners();
        for (int i = 0; i < owner.size(); i++) {
            this.getRegion().addMember(owner.get(i));
        }
        this.getRegion().setOwner(member);
    }

    public String getDimensions(){
        Vector min = this.getRegion().getMinPoint();
        Vector max = this.getRegion().getMaxPoint();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int maxZ = max.getBlockZ();
        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        return Math.abs((maxX - minX) + 1) + "x" + (Math.abs(maxY - minY) + 1) + "x" + (Math.abs(maxZ - minZ) + 1);

    }

    public int timeSinceLastReset(){
        GregorianCalendar calendar = new GregorianCalendar();
        long result = calendar.getTimeInMillis() - this.lastreset;
        int days = (int) (result / (1000 * 60 * 60 * 24));
        return days;
    }

    public void userBlockReset(Player player){
        this.resetBlocks();
        GregorianCalendar calendar = new GregorianCalendar();
        this.lastreset = calendar.getTimeInMillis();
        RegionManager.writeRegionsToConfig();
        player.sendMessage(Messages.PREFIX + Messages.RESET_COMPLETE);
    }

    public static void setResetcooldown(int cooldown){
        Region.resetcooldown = cooldown;
    }

    public static int getResetCooldown(){
        return Region.resetcooldown;
    }

    public int getRemainingDaysTillReset(){
        ArrayList<UUID> ownerlist = this.getRegion().getOwners();
        if(ownerlist.size() > 0) {
            UUID owner = ownerlist.get(0);
            if(AdvancedRegionMarket.getEnableTakeOver() ||AdvancedRegionMarket.getEnableAutoReset()) {
                try{
                    ResultSet rs = AdvancedRegionMarket.getStmt().executeQuery("SELECT * FROM `" + AdvancedRegionMarket.getSqlPrefix() + "lastlogin` WHERE `uuid` = '" + owner.toString() + "'");

                    if(rs.next()){
                        Timestamp lastlogin = rs.getTimestamp("lastlogin");
                        GregorianCalendar calendar = new GregorianCalendar();

                        long time = lastlogin.getTime();
                        long result = calendar.getTimeInMillis() - time;
                        int days = (int)  (AdvancedRegionMarket.getAutoResetAfter() - (result / (1000 * 60 * 60 * 24)));
                        return days;
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return 0;
    }

    public void writeSigns(){
        for (int i = 0; i < this.sellsign.size(); i++) {
            this.updateSignText(this.sellsign.get(i));
        }
    }

    public abstract void setSold(OfflinePlayer player);
    protected abstract void updateSignText(Sign mysign);
    public abstract void buy(Player player) throws InputException;
    public abstract void userSell(Player player);
    public abstract double getPaybackMoney();

    public void resetRegion(){

        this.unsell();

        this.resetBlocks();
        return;
    }

    public void automaticResetRegion(){
        this.automaticResetRegion(null);
    }

    public void automaticResetRegion(Player player){

        this.unsell();
        if(this.isDoBlockReset()){
            this.resetBlocks();
        }
        if(player != null) {
            player.sendMessage(Messages.PREFIX + Messages.RESET_COMPLETE);
        }
    }

    public boolean isHotel() {
        return isHotel;
    }

    public boolean allowBlockBreak(Location breakloc) {
        if(this.isHotel) {
            for (int i = 0; i < this.builtblocks.size(); i++) {
                if (this.builtblocks.get(i).distance(breakloc) < 1) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public void setHotel(Boolean bool) {
        this.isHotel = bool;
        RegionManager.writeRegionsToConfig();
    }

    public void unsell(){
        this.getRegion().deleteMembers();
        this.getRegion().deleteOwners();
        this.sold = false;
        this.lastreset = 1;

        for(int i = 0; i < this.sellsign.size(); i++){
            this.updateSignText(this.sellsign.get(i));
        }
        RegionManager.writeRegionsToConfig();
    }

    public boolean isDoBlockReset() {
        return isDoBlockReset;
    }

    public void setDoBlockReset(Boolean bool) {
        this.isDoBlockReset = bool;
        RegionManager.writeRegionsToConfig();
    }

    public static void setCompleteTabRegions(Boolean bool) {
        Region.completeTabRegions = bool;
    }

    public List<Region> getSubregions() {
        return subregions;
    }

    protected List<Sign> getSellSigns() {
        return this.sellsign;
    }

    protected long getLastreset() {
        return this.lastreset;
    }

    protected boolean isAutoreset() {
        return this.autoreset;
    }
}
