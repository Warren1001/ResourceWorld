package com.kabryxis.resourceworld;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class PortalListener implements Listener {
	
	private final Set<Gate> overworldNetherGates = new HashSet<>();
	private final Set<Player> waitingPlayers = new HashSet<>();
	
	private final ResourceWorld plugin;
	
	public PortalListener(ResourceWorld plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPortalLight(BlockIgniteEvent event) {
		if(event.getCause() == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL && plugin.attemptToLightPortal(event.getBlock())) event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerPortal(PlayerPortalEvent event) {
		if(event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
			Gate gate = plugin.findGate(event.getFrom().clone());
			Player player = event.getPlayer();
			if(gate != null) {
				event.setCancelled(true);
				boolean success = gate.teleportThrough(player);
				if(success) waitingPlayers.remove(player);
				else waitingPlayers.add(player);
			}
			else if(event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) createNetherGate(event.getFrom());
			else if(event.getFrom().getWorld().getEnvironment() == World.Environment.NETHER) {
				Location overworldFrom = event.getFrom();
				overworldFrom.setX(overworldFrom.getBlockX() * 8);
				overworldFrom.setX(overworldFrom.getBlockZ() * 8);
				overworldFrom.setWorld(event.getTo().getWorld());
				Gate overworldGate = findOverworldGate(overworldFrom);
				boolean isX = overworldGate.getAxis() == Axis.X;
				event.setTo(overworldGate.getLocation().getBlock().getRelative(isX ? 1 : 0, 1, isX ? 0 : 1).getLocation());
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onEntityPortalEnter(EntityPortalEnterEvent event) {
		Entity entity = event.getEntity();
		if(!(entity instanceof Player)) return;
		Player player = (Player)entity;
		if(!waitingPlayers.contains(player)) return;
		Gate gate = plugin.findGate(event.getLocation());
		if(gate != null && gate.teleportThrough(player)) waitingPlayers.remove(player);
	}
	
	private Gate createNetherGate(Location loc) {
		Block block = LocationHelper.searchForPortalBlock(loc);
		Gate g = overworldNetherGates.stream().filter(gate -> gate.belongsToPortal(block)).findFirst().orElse(LocationHelper.getNewGateOfGateConstruction(block));
		overworldNetherGates.add(g);
		return g;
	}
	
	private Gate findOverworldGate(Location loc) {
		return overworldNetherGates.stream().min(Comparator.comparingInt(gate -> (int)gate.getLocation().distanceSquared(loc))).orElse(findOverworldGateManually(loc));
	}
	
	private Gate findOverworldGateManually(Location loc) {
		int minX = loc.getBlockX() - 128;
		int maxX = loc.getBlockX() + 128;
		int minY = Math.max(0, loc.getBlockY() - 64);
		int maxY = Math.min(loc.getWorld().getMaxHeight(), loc.getBlockY() + 64);
		int minZ = loc.getBlockZ() - 128;
		int maxZ = loc.getBlockZ() + 128;
		for(int x = minX; x <= maxX; x++) {
			for(int z = minZ; z <= maxZ; z++) {
				for(int y = minY; y <= maxY; y++) {
					Block block = loc.getBlock();
					if(block.getType() == Material.NETHER_PORTAL) {
						Gate gate = plugin.findGate(loc);
						if(gate != null) continue;
						gate = LocationHelper.getNewGateOfGateConstruction(block);
						if(gate.isNetherPortal()) overworldNetherGates.add(gate);
					}
				}
			}
		}
		return overworldNetherGates.stream().min(Comparator.comparingInt(gate -> (int)gate.getLocation().distanceSquared(loc))).orElse(null);
	}
	
}
