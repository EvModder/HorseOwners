package net.evmodder.HorseOwners.listeners;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import net.evmodder.HorseOwners.HorseUtils;
import net.evmodder.HorseOwners.HorseManager;
import net.evmodder.HorseOwners.api.events.HorseDeathEvent;

public class DeathListener implements Listener{

	//Clear stats on death
	@EventHandler
	public void onDeath(EntityDeathEvent evt){
		if(evt.getEntity() instanceof AbstractHorse && evt.getEntity().getCustomName() != null
				&& HorseManager.getPlugin().getAPI().isClaimableHorseType(evt.getEntity())){
			final AbstractHorse horse = (AbstractHorse)evt.getEntity();

			HorseDeathEvent horseDeathEvent = new HorseDeathEvent(horse, evt.getDrops(), evt.getDroppedExp());
			HorseManager.getPlugin().getServer().getPluginManager().callEvent(horseDeathEvent);
			evt.setDroppedExp(horseDeathEvent.getDroppedExp());
			HorseManager.getPlugin().getAPI().removeHorse(HorseUtils.cleanName(horse.getCustomName()));
		}
	}
}