package net.evmodder.HorseOwners.listeners;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import net.evmodder.HorseOwners.HorseUtils;
import net.evmodder.HorseOwners.HorseManager;

public class SpawnListener implements Listener{
	final HorseManager plugin;
	final double MAX_JUMP, MAX_SPEED, MAX_HEALTH;

	public SpawnListener(){
		plugin = HorseManager.getPlugin();
		double normalJump = plugin.getConfig().getDouble("max-jump", 5.29);
		double normalSpeed = plugin.getConfig().getDouble("max-speed", 14.5125);
		double normalHealth = plugin.getConfig().getDouble("max-health", 30);
		MAX_JUMP = normalJump > 0 ? HorseUtils.denormalizeJump(normalJump) : -1;
		MAX_SPEED = normalSpeed > 0 ? HorseUtils.denormalizeSpeed(normalSpeed) : -1;
		MAX_HEALTH = normalHealth > 0 ? normalHealth : -1;
	}

	// Used to limit stats of naturally spawning horses (as per config settings)
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onAbstractHorseSpawn(CreatureSpawnEvent evt){
		if(evt.getEntity() instanceof AbstractHorse){
			plugin.getLogger().info("CreatureSpawnEvent called");
			//if(evt.isCancelled()) return; // Still useful as a reference for later EventHandlers
			/*switch(evt.getSpawnReason()){
				case SPAWNER:
				case SPAWNER_EGG:
				case DISPENSE_EGG:
				case LIGHTNING:
				case NATURAL:// DEFAULT, CUSTOM
				case BREEDING:
					break;
				default:
					return;
			}*/
			AbstractHorse horse = (AbstractHorse) evt.getEntity();
			//limit jump
			if(MAX_JUMP != -1 && horse.getJumpStrength() > MAX_JUMP) horse.setJumpStrength(MAX_JUMP);
	
			//limit speed
			double speed = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
			if(MAX_SPEED != -1 && speed > MAX_SPEED) speed = MAX_SPEED;
			horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
	
			//limit health
			double health = horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			if(MAX_HEALTH != -1 && health > MAX_HEALTH) health = MAX_HEALTH;
			horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
			horse.setHealth(health);
		}
		// Used to rank/record stats for wild horses (rankUnclaimed)
		if(!evt.isCancelled() && plugin.getAPI().isClaimableHorseType(evt.getEntity())){
			HorseUtils.setSpawnReason(evt.getEntity(), evt.getSpawnReason());
			HorseUtils.setTimeBorn(evt.getEntity(), System.currentTimeMillis());
			plugin.getAPI().updateDatabase(evt.getEntity());
		}
	}
}