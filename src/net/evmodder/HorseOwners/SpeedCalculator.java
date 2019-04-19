package net.evmodder.HorseOwners;

import org.bukkit.entity.Entity;
import net.evmodder.EvLib.ReflectionUtils;
import net.evmodder.EvLib.ReflectionUtils.RefClass;
import net.evmodder.EvLib.ReflectionUtils.RefMethod;

public class SpeedCalculator {
	private static final RefClass classCraftLivingEntity = ReflectionUtils.getRefClass("{cb}.entity.CraftLivingEntity");
	private static final RefClass classEntityInsentient = ReflectionUtils.getRefClass("{nms}.EntityInsentient");
	private static final RefClass classGenericAttributes = ReflectionUtils.getRefClass("{nms}.GenericAttributes");
	private static final RefClass classIAttribute = ReflectionUtils.getRefClass("{nms}.IAttribute");
	private static final RefClass classAttributeInstance = ReflectionUtils.getRefClass("{nms}.AttributeInstance");
	private RefMethod methodGetHandle = classCraftLivingEntity.getMethod("getHandle");
	private RefMethod methodGetAttributeInstance = classEntityInsentient.getMethod("getAttributeInstance", classIAttribute);
	private Object movementSpeedEnumVal = classGenericAttributes.getField("MOVEMENT_SPEED").of(null).get();
	private RefMethod methodGetValue = classAttributeInstance.getMethod("getValue");
	private RefMethod methodSetValue = classAttributeInstance.getMethod("setValue", double.class);

	/*short[] version;//ie, [1,8,8] [1,7,10]
	public SpeedCalculator(String v){
		//String[] parts = v.split("-")[0].split(".");
		
		//version = new short[parts.length];
		//for(int i=0; i<parts.length; ++i) version[i] = Short.parseShort(parts[i]);
		
		//if(version[1] < 8);//TODO: error!
		//else if(version[1] == 8 && version[2] < 3) movementSpeedField = classGenericAttributes.getField("d");
		//else{
//			Object clss = classGenericAttributes.getConstructor().create();
//			movementSpeed = classGenericAttributes.getField("MOVEMENT_SPEED").of(clss).get();
		//}
	}*/
	
	public double getHorseSpeed(Entity h){
//		if(version < 180) return -1;
//		else if(version <= 182) return getHorseSpeed_v1_8_R1(h);
//		else if(version <= 188) return getHorseSpeed_v1_8_R3(h);
//		else return -1;
		
		//return ((EntityInsentient)((CraftLivingEntity)h).getHandle()).getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).getValue();
		
		Object handle = methodGetHandle.of(h).call();
		Object attribute = methodGetAttributeInstance.of(handle).call(movementSpeedEnumVal);
		return (Double) methodGetValue.of(attribute).call();
		
//		return (Double) methodGetValue.of(
//				methodGetAttributeInstance.of(methodGetHandle.of(h).call()).call(movementSpeedField)).call();
	}
	public void setHorseSpeed(Entity h, double speed){
//		if(version < 180);//Error!
//		else if(version <= 182) setHorseSpeed_v1_8_R1(h, speed);
//		else if(version <= 188) setHorseSpeed_v1_8_R3(h, speed);

//		((EntityInsentient)((CraftLivingEntity)h).getHandle()).getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(speed);

		//use about .225 for normalish speed
		Object handle = methodGetHandle.of(h).call();
		Object attribute = methodGetAttributeInstance.of(handle).call(movementSpeedEnumVal);
		methodSetValue.of(attribute).call(speed);

//		methodSetValue.of(methodGetAttributeInstance.of(methodGetHandle.of(h).call()).call(movementSpeedField)).call(speed);
	}
	
/*	// Getters --------------------------------------------------------
	public double getHorseSpeed_v1_8_R1(Horse h){
		net.minecraft.server.v1_8_R1.AttributeInstance attributes
		= ((net.minecraft.server.v1_8_R1.EntityInsentient)((org.bukkit.craftbukkit.v1_8_R1.entity.CraftLivingEntity)h)
		.getHandle()).getAttributeInstance(net.minecraft.server.v1_8_R1.GenericAttributes.d);
		return attributes.getValue();
	}
	public double getHorseSpeed_v1_8_R3(Horse h){
		net.minecraft.server.v1_8_R3.AttributeInstance attributes
		= ((net.minecraft.server.v1_8_R3.EntityInsentient)((org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity)h)
		.getHandle()).getAttributeInstance(net.minecraft.server.v1_8_R3.GenericAttributes.MOVEMENT_SPEED);
		return attributes.getValue();
	}
	// Setters --------------------------------------------------------
	public void setHorseSpeed_v1_8_R1(Horse h, double speed){
		net.minecraft.server.v1_8_R1.AttributeInstance attributes
		= ((net.minecraft.server.v1_8_R1.EntityInsentient)((org.bukkit.craftbukkit.v1_8_R1.entity.CraftLivingEntity)h)
		.getHandle()).getAttributeInstance(net.minecraft.server.v1_8_R1.GenericAttributes.d);
		attributes.setValue(speed);
	}
	public void setHorseSpeed_v1_8_R3(Horse h, double speed){
		net.minecraft.server.v1_8_R3.AttributeInstance attributes
		= ((net.minecraft.server.v1_8_R3.EntityInsentient)((org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity)h)
		.getHandle()).getAttributeInstance(net.minecraft.server.v1_8_R3.GenericAttributes.MOVEMENT_SPEED);
		attributes.setValue(speed);
	}*/
}
