package net.evmodder.HorseOwners.listeners;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import net.evmodder.HorseOwners.HorseManager;

public class DeathListener implements Listener{

	//Clear stats on death
	@EventHandler
	public void onDeath(EntityDeathEvent evt){
		if(evt.getEntity() instanceof AbstractHorse && evt.getEntity().getCustomName() != null
				&& HorseManager.getPlugin().isClaimableHorseType(evt.getEntity())){
			HorseManager.getPlugin().removeHorse(evt.getEntity().getCustomName(), true);
		}
	}
}