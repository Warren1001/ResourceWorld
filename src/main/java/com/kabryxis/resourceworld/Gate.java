package com.kabryxis.resourceworld;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class Gate {
	
	private final Set<Block> frameBlocks = new HashSet<>(10);
	private final Set<Block> portalBlocks = new HashSet<>(6);
	private final Set<Block> cornerFrameBlocks = new HashSet<>(4);
	
	private final Set<Player> waitingPlayers = new HashSet<>();
	
	private final Axis axis;
	
	private Portal portal = null;
	private Location location = null;
	private boolean hasBeenConstructed;
	
	public Gate(Portal portal) {
		this.portal = portal;
		this.axis = portal.getAxis();
		this.hasBeenConstructed = false;
	}
	
	public Gate(Portal portal, Location location) {
		this.portal = portal;
		this.axis = portal.getAxis();
		this.location = location;
		findPortalBlocks();
		this.hasBeenConstructed = true;
	}
	
	public Gate(Portal portal, String serializedString) {
		this.portal = portal;
		this.axis = portal.getAxis();
		String[] args = serializedString.split(":");
		this.location = LocationHelper.deserialize(args[0]);
		findPortalBlocks();
		this.hasBeenConstructed = Boolean.parseBoolean(args[1]) || (isFrameConstructed0() && isLit());
	}
	
	public Gate(Axis axis, Location location) {
		this.axis = axis;
		this.location = location;
		findPortalBlocks();
		this.hasBeenConstructed = true;
	}
	
	public Location getLocation() {
		return location == null ? null : location.clone();
	}
	
	public void findLocation() {
		if(location != null) return;
		location = LocationHelper.findOtherGateLocation(getOtherGate());
		findPortalBlocks();
	}
	
	public Axis getAxis() {
		return axis;
	}
	
	private void findPortalBlocks() {
		boolean isX = portal.getAxis() == Axis.X;
		Block startingBlock = location.getBlock();
		for(int offsetXZ = 0; offsetXZ < 4; offsetXZ++) {
			for(int offsetY = 0; offsetY < 5; offsetY++) {
				Block block = startingBlock.getRelative(isX ? offsetXZ : 0, offsetY, isX ? 0 : offsetXZ);
				if((offsetXZ == 1 || offsetXZ == 2) && (offsetY > 0 && offsetY < 4)) portalBlocks.add(block);
				else if(((offsetXZ == 0 || offsetXZ == 3) && (offsetY == 0 || offsetY == 4))) cornerFrameBlocks.add(block);
				else frameBlocks.add(block);
			}
		}
	}
	
	public void setPortal(Portal portal) {
		this.portal = portal;
	}
	
	public Gate getOtherGate() {
		return portal.getOtherGate(this);
	}
	
	public boolean isFrameConstructed() {
		return hasBeenConstructed && isFrameConstructed0();
	}
	
	private boolean isFrameConstructed0() {
		return frameBlocks.stream().allMatch(block -> block.getType() == LocationHelper.PORTAL_FRAME_TYPE);
	}
	
	public boolean isLit() {
		return portalBlocks.stream().allMatch(block -> block.getType() == Material.NETHER_PORTAL);
	}
	
	public boolean belongsToPortal(Block block) {
		return frameBlocks.contains(block) || portalBlocks.contains(block) || cornerFrameBlocks.contains(block);
	}
	
	public void construct() {
		if(isFrameConstructed()) return;
		frameBlocks.forEach(block -> block.setType(LocationHelper.PORTAL_FRAME_TYPE));
		cornerFrameBlocks.forEach(block -> block.setType(LocationHelper.PORTAL_FRAME_TYPE));
		portalBlocks.forEach(block -> {
			boolean isX = portal.getAxis() == Axis.X;
			block.getRelative(isX ? 0 : 1, 0, isX ? 1 : 0).setType(Material.AIR);
			block.getRelative(isX ? 0 : -1, 0, isX ? -1 : 0).setType(Material.AIR);
		});
		hasBeenConstructed = true;
		light();
	}
	
	public boolean light() {
		if(!isFrameConstructed()) return false;
		if(isLit()) return true;
		portalBlocks.forEach(block -> {
			block.setType(Material.NETHER_PORTAL);
			Orientable data = (Orientable)block.getBlockData();
			data.setAxis(portal.getAxis());
			block.setBlockData(data);
			block.getWorld().getPlayers().forEach(player -> player.sendBlockChange(block.getLocation(), data));
		});
		return true;
	}
	
	public boolean teleportThrough(Player player) {
		Gate otherGate = getOtherGate();
		if(otherGate.location == null) return false;
		if(!otherGate.isFrameConstructed()) otherGate.construct();
		else if(!otherGate.isLit()) otherGate.light();
		Location to = player.getLocation().subtract(location.toVector()).add(otherGate.location.toVector());
		to.setWorld(otherGate.location.getWorld());
		player.teleport(to);
		return true;
	}
	
	public String serialize() {
		return LocationHelper.serialize(location) + ":" + hasBeenConstructed;
	}
	
	@Override
	public String toString() {
		return String.format("Gate[location=%s]", location);
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof Gate && ((Gate)obj).location.equals(location);
	}
	
}
