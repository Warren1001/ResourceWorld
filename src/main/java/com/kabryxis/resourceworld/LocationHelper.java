package com.kabryxis.resourceworld;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Orientable;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class LocationHelper {
	
	public static final int[][] VALID_RELATIVES_XZ =  { { -1, 2 }, { -2, 1 } };
	public static final int[][] VALID_RELATIVES_Y =  { { -3, 1 }, { -2, 2 }, { -1, 3 } };
	public static final boolean[] IS_XS = { true, false };
	
	public static Material PORTAL_FRAME_TYPE;
	public static int PORTAL_SPAWN_RADIUS;
	public static int PORTAL_RELATIVE_SPAWN_RADIUS;
	public static World RESOURCE_WORLD;
	
	private LocationHelper() {}
	
	public static Portal findUncreatedPortalToLight(Block ignitedBlock) {
		Portal portal = null;
		boolean found = false;
		for(int[] relativesY : VALID_RELATIVES_Y) {
			for(int[] relativeXorZ : VALID_RELATIVES_XZ) {
				for(boolean isX : IS_XS) {
					Set<Block> airBlocks = getAirBlocksAtValidPortal(ignitedBlock, isX, relativeXorZ[0], relativeXorZ[1], relativesY[0], relativesY[1]);
					if(!airBlocks.isEmpty()) {
						found = true;
						portal = new Portal(isX ? Axis.X : Axis.Z, ignitedBlock.getRelative(isX ? relativeXorZ[0] : 0, relativesY[0], isX ? 0 : relativeXorZ[0]).getLocation());
						break;
					}
				}
				if(found) break;
			}
			if(found) break;
		}
		return portal;
	}
	
	public static Set<Block> getAirBlocksAtValidPortal(Block original, boolean isX, int relativeMinXorZ, int relativeMaxXorZ, int relativeMinY, int relativeMaxY) {
		Set<Block> airBlocks = new HashSet<>();
		int portalBlockCount = 0;
		for(int xorz = relativeMinXorZ; xorz <= relativeMaxXorZ; xorz++) {
			for(int y = relativeMinY; y <= relativeMaxY; y++) {
				Block block = original.getRelative(isX ? xorz : 0, y, isX ? 0 : xorz);
				if((xorz != relativeMinXorZ && xorz != relativeMaxXorZ) && (y != relativeMinY && y != relativeMaxY)) {
					if(block.getType() == Material.AIR) airBlocks.add(block);
				}
				else if(!((xorz == relativeMinXorZ || xorz == relativeMaxXorZ) && (y == relativeMinY || y == relativeMaxY)) && block.getType() == PORTAL_FRAME_TYPE) portalBlockCount++;
			}
		}
		return airBlocks.size() == 6 && portalBlockCount == 10 ? airBlocks : Collections.emptySet();
	}
	
	public static Location findOtherGateLocation(Gate gate1) {
		Location loc = gate1.getLocation().clone();
		loc.setWorld(RESOURCE_WORLD);
		int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
		Vector distance = loc.toVector().subtract(RESOURCE_WORLD.getSpawnLocation().toVector());
		if(distance.getBlockX() > PORTAL_SPAWN_RADIUS || distance.getBlockZ() > PORTAL_SPAWN_RADIUS) {
			double ratio = (double)x / z;
			int bound = PORTAL_SPAWN_RADIUS / 2 - PORTAL_RELATIVE_SPAWN_RADIUS;
			int c1 = new Random().nextInt(bound) + bound;
			if(ratio >= 1) {
				x = c1;
				z = (int)(x / ratio);
			}
			else {
				z = c1;
				x = (int)(z * ratio);
			}
		}
		loc = checkValidSpawn(x, y, z);
		long start = System.currentTimeMillis();
		Random random = new Random();
		while(loc == null) {
			int rxo = random.nextInt(PORTAL_RELATIVE_SPAWN_RADIUS) - PORTAL_RELATIVE_SPAWN_RADIUS / 2;
			int rzo = random.nextInt(PORTAL_RELATIVE_SPAWN_RADIUS) - PORTAL_RELATIVE_SPAWN_RADIUS / 2;
			loc = checkValidSpawn(x + rxo, y, z + rzo);
		}
		long end = System.currentTimeMillis() - start;
		return loc;
	}
	
	public static Location checkValidSpawn(int x, int y, int z) {
		Block block = RESOURCE_WORLD.getBlockAt(x, Math.max(y, 6), z);
		Vector distance = block.getLocation().toVector().subtract(RESOURCE_WORLD.getSpawnLocation().toVector());
		if(distance.getBlockX() > PORTAL_SPAWN_RADIUS || distance.getBlockZ() > PORTAL_SPAWN_RADIUS) return null;
		if(block.getType() == Material.AIR) {
			for(; y > 5; y--) {
				block = RESOURCE_WORLD.getBlockAt(x, y, z);
				if(block.getType() != Material.AIR) break;
			}
			if(block.getType() == Material.AIR) return null;
		}
		if(block.getType() == Material.LAVA) {
			for(; y < 116; y++) {
				block = RESOURCE_WORLD.getBlockAt(x, y, z);
				if(block.getType() != Material.LAVA && block.getType() != Material.AIR) break;
			}
			if(block.getType() == Material.AIR || block.getType() == Material.LAVA) return null;
		}
		return block.getLocation();
	}
	
	public static Block searchForPortalBlock(Location loc) {
		Block block = loc.getBlock();
		if(block.getType() == Material.NETHER_PORTAL) return block;
		Set<Block> blocks = new HashSet<>(4);
		double offsetX = loc.getX() - loc.getBlockX(), offsetZ = loc.getZ() - loc.getBlockZ();
		boolean two = false, lessX = offsetX <= 0.3, lessZ = offsetZ <= 0.3;
		blocks.add(block);
		if(lessX || offsetX >= 0.7) { // positive x is east, positive z is south
			Block b = block.getRelative(lessX ? BlockFace.WEST : BlockFace.EAST);
			blocks.add(b);
			two = true;
		}
		if(lessZ || offsetZ >= 0.7) {
			block = block.getRelative(lessZ ? BlockFace.NORTH : BlockFace.SOUTH);
			blocks.add(block);
			if(two) blocks.add(block.getRelative(lessX ? BlockFace.WEST : BlockFace.EAST));
		}
		return blocks.stream().filter(b -> b.getType() == Material.NETHER_PORTAL).findFirst().orElse(null);
	}
	
	public static Gate getNewGateOfGateConstruction(Block block) {
		Block blockTest = block.getRelative(BlockFace.DOWN);
		while(blockTest.getType() == Material.NETHER_PORTAL) {
			block = blockTest;
			blockTest = block.getRelative(BlockFace.DOWN);
		}
		Axis axis = ((Orientable)block.getBlockData()).getAxis();
		boolean isX = axis == Axis.X;
		blockTest = block.getRelative(isX ? -1 : 0, 0, isX ? 0 : -1);
		while(blockTest.getType() == Material.NETHER_PORTAL) {
			block = blockTest;
			blockTest = block.getRelative(isX ? -1 : 0, 0, isX ? 0 : -1);
		}
		block = block.getRelative(isX ? -1 : 0, -1, isX ? 0 : -1);
		return new Gate(axis, block.getLocation());
	}
	
	public static String serialize(Location loc) {
		return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
	}
	
	public static Location deserialize(String locString) {
		String[] args = locString.split(",");
		return new Location(Bukkit.getWorld(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
	}
	
}
