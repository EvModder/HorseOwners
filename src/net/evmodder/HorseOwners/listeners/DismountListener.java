package net.evmodder.HorseOwners.listeners;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.entity.EntityDismountEvent;
import net.evmodder.HorseOwners.HorseManager;

public class DismountListener implements Listener{
	//Clear stats on death
	@EventHandler
	public void onHorseDismount(EntityDismountEvent evt){
		HorseManager.getPlugin().getLogger().info(evt.getEntity().getType()+" dsm "+evt.getDismounted().getType());
		if(evt.getEntity() instanceof AbstractHorse && evt.getEntity().getCustomName() != null
				&& HorseManager.getPlugin().isClaimableHorseType(evt.getEntity())){
			HorseManager.getPlugin().updateData((AbstractHorse)evt.getEntity());
		}
	}
}