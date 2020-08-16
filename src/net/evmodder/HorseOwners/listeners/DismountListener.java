package net.evmodder.HorseOwners.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.entity.EntityDismountEvent;
import net.evmodder.HorseOwners.HorseManager;

public class DismountListener implements Listener{
	//Clear stats on death
	@EventHandler
	public void onHorseDismount(EntityDismountEvent evt){
		if(HorseManager.getPlugin().getAPI().isClaimableHorseType(evt.getDismounted())){
			HorseManager.getPlugin().getAPI().updateDatabase(evt.getDismounted());
		}
	}
}