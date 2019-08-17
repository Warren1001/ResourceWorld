package com.kabryxis.resourceworld;

import org.apache.commons.lang.Validate;
import org.bukkit.Axis;
import org.bukkit.Location;

public class Portal {
	
	private final Axis axis;
	private final Gate gate1;
	
	private Gate gate2;
	
	public Portal(Axis axis, Location loc1) {
		this.axis = axis;
		this.gate1 = new Gate(this, loc1);
		this.gate2 = new Gate(this);
	}
	
	public Portal(Gate gate1) {
		this.axis = gate1.getAxis();
		this.gate1 = gate1;
		gate1.setPortal(this);
		this.gate2 = new Gate(this);
	}
	
	public Portal(String serializedString) {
		String[] args = serializedString.split(";");
		axis = Axis.valueOf(args[0].toUpperCase());
		gate1 = new Gate(this, args[1]);
		gate2 = new Gate(this, args[2]);
	}
	
	public Axis getAxis() {
		return axis;
	}
	
	public Gate getGate1() {
		return gate1;
	}
	
	public Gate getGate2() {
		return gate2;
	}
	
	public Gate getOtherGate(Gate gate) {
		Validate.isTrue(gate == gate1 || gate == gate2, "The provided gate is not associated with this portal");
		return gate == gate1 ? gate2 : gate1;
	}
	
	public String serialize() {
		return axis.name() + ";" + gate1.serialize() + ";" + gate2.serialize();
	}
	
	@Override
	public String toString() {
		return String.format("Portal[axis=%s,gate1=%s,gate2=%s]", axis, gate1, gate2);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Portal) {
			Portal portal = (Portal)obj;
			return portal.axis == axis && (portal.gate1.equals(gate1) || portal.gate2.equals(gate2));
		}
		return false;
	}
	
}
