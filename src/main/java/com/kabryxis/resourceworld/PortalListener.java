package com.kabryxis.resourceworld;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashSet;
import java.util.Set;

public class PortalListener implements Listener {
	
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
			Gate gate = plugin.findGate(event.getFrom());
			Player player = event.getPlayer();
			if(gate != null) {
				event.setCancelled(true);
				boolean success = gate.teleportThrough(player);
				if(success) waitingPlayers.remove(player);
				else waitingPlayers.add(player);
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
	
}
