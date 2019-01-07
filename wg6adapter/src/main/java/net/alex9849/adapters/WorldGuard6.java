package net.alex9849.adapters;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import net.alex9849.inter.WGRegion;
import net.alex9849.inter.WorldGuardInterface;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorldGuard6 extends WorldGuardInterface {

    @Override
    public RegionManager getRegionManager(World world, WorldGuardPlugin worldGuardPlugin) {
        return worldGuardPlugin.getRegionManager(world);
    }

    public WG6Region getRegion(World world, WorldGuardPlugin worldGuardPlugin, String regionID) {
        RegionManager regionManager = this.getRegionManager(world, worldGuardPlugin);
        if(regionManager == null) {
            return null;
        }

        ProtectedRegion region = regionManager.getRegion(regionID);
        if(region == null) {
            return null;
        }

        return new WG6Region(region);
    }

    public boolean canBuild(Player player, Location location, WorldGuardPlugin worldGuardPlugin){
        return worldGuardPlugin.canBuild(player, location);
    }

    @Override
    public WGRegion createRegion(WGRegion parentRegion, World world, String regionID, Location pos1, Location pos2, WorldGuardPlugin worldGuardPlugin) {
        WG6Region wg6Region = (WG6Region) parentRegion;
        ProtectedRegion protectedRegion = new ProtectedCuboidRegion(regionID, new BlockVector(pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ()), new BlockVector(pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ()));
        try {
            protectedRegion.setParent(wg6Region.getRegion());
        } catch (ProtectedRegion.CircularInheritanceException e) {
            e.printStackTrace();
        }
        WG6Region returnRegion = new WG6Region(protectedRegion);
        return returnRegion;
    }

    @Override
    public List<WGRegion> getApplicableRegions(World world, Location loc, WorldGuardPlugin worldGuardPlugin) {
        List<ProtectedRegion> protectedRegions = new ArrayList<ProtectedRegion>(worldGuardPlugin.getRegionManager(world).getApplicableRegions(loc).getRegions());
        List<WGRegion> wg6Regions = new ArrayList<WGRegion>();
        for(ProtectedRegion pRegion : protectedRegions) {
            wg6Regions.add(new WG6Region(pRegion));
        }
        return wg6Regions;
    }

    @Override
    public void addToRegionManager(WGRegion region, World world, WorldGuardPlugin worldGuardPlugin) {
        WG6Region wg6Region = (WG6Region) region;
        getRegionManager(world, worldGuardPlugin).addRegion(wg6Region.getRegion());
    }

}
