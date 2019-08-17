package com.kabryxis.resourceworld;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ResourceWorld extends JavaPlugin {
	
	private final Set<Portal> portals = new HashSet<>();
	private final Set<String> allowedWorlds = new HashSet<>();
	
	@Override
	public void onEnable() {
		saveDefaultConfig();
		String portalBlockString = getConfig().getString("portal-block");
		if(portalBlockString == null) {
			disablePlugin("Missing value in config: [portal-block: Material_TYPE]");
			return;
		}
		LocationHelper.PORTAL_FRAME_TYPE = Material.matchMaterial(portalBlockString);
		if(LocationHelper.PORTAL_FRAME_TYPE == null) {
			disablePlugin("Could not find material %s", portalBlockString);
			return;
		}
		LocationHelper.PORTAL_SPAWN_RADIUS = getConfig().getInt("portal-spawn-radius");
		if(LocationHelper.PORTAL_SPAWN_RADIUS <= 24) {
			disablePlugin("Invalid or no value provided for portal-spawn-radius. Must be an int greater than 24.");
			return;
		}
		LocationHelper.PORTAL_SPAWN_RADIUS -= 4;
		LocationHelper.PORTAL_RELATIVE_SPAWN_RADIUS = getConfig().getInt("portal-relative-spawn-radius", 500);
		String resourceWorldName = getConfig().getString("world-name", "world_resource");
		if(resourceWorldName.isEmpty()) {
			disablePlugin("Cannot create a resource world with an empty name.");
			return;
		}
		LocationHelper.RESOURCE_WORLD = new WorldCreator(resourceWorldName).createWorld();
		LocationHelper.RESOURCE_WORLD.setSpawnLocation(0, LocationHelper.RESOURCE_WORLD.getHighestBlockYAt(0, 0), 0);
		Object allowedWorldsObj = getConfig().get("allowed-world-names");
		if(allowedWorldsObj instanceof String) allowedWorlds.add(allowedWorldsObj.toString().toLowerCase());
		else if(allowedWorldsObj instanceof List<?>) ((List<?>)allowedWorldsObj).forEach(obj -> allowedWorlds.add(obj.toString().toLowerCase()));
		else {
			disablePlugin("allowed-world-names must be a String or List containing allowed world names for portal creation");
			return;
		}
		loadPortalsFromDisk();
		getServer().getPluginManager().registerEvents(new PortalListener(this), this);
	}
	
	@Override
	public void onDisable() {
		savePortalsToDisk();
	}
	
	public void loadPortalsFromDisk() {
		portals.clear();
		List<String> lines;
		try {
			Path path = new File(getDataFolder(), "portals.txt").toPath();
			if(!path.toFile().exists()) return;
			lines = Files.readAllLines(path);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		lines.forEach(line -> portals.add(new Portal(line)));
	}
	
	public void savePortalsToDisk() {
		if(portals.isEmpty()) return;
		try {
			Files.write(new File(getDataFolder(), "portals.txt").toPath(), portals.stream().map(Portal::serialize).collect(Collectors.toSet()));
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public Gate findGate(Location loc) {
		Block block = LocationHelper.searchForPortalBlock(loc);
		Gate gate = findGate0(block);
		if(gate != null) return gate;
		gate = LocationHelper.getNewGateOfGateConstruction(block);
		if(gate.isFrameConstructed()) {
			Portal portal = new Portal(gate);
			getServer().getScheduler().runTask(this, portal.getGate2()::findLocation);
			portals.add(portal);
			return gate;
		}
		return null;
	}
	
	private Gate findGate0(Block block) {
		for(Portal portal : portals) {
			Gate gate1 = portal.getGate1();
			Gate gate2 = portal.getGate2();
			if(gate1.belongsToPortal(block)) return gate1;
			if(gate2.belongsToPortal(block)) return gate2;
		}
		return null;
	}
	
	public boolean attemptToLightPortal(Block block) {
		Gate gate = findGate0(block);
		if(gate != null) {
			if(gate.isFrameConstructed()) {
				gate.light();
				return true;
			}
			return false;
		}
		if(!allowedWorlds.contains(block.getWorld().getName().toLowerCase())) return false;
		Portal portal = LocationHelper.findUncreatedPortalToLight(block);
		if(portal == null) return false;
		getServer().getScheduler().runTask(this, portal.getGate2()::findLocation);
		portals.add(portal);
		getServer().getScheduler().runTaskAsynchronously(this, this::savePortalsToDisk);
		portal.getGate1().light();
		return true;
	}
	
	private void disablePlugin(String message, Object... objects) {
		getLogger().severe(String.format(message, objects));
		getServer().getPluginManager().disablePlugin(this);
	}
	
}
