package net.evmodder.HorseOwners.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import net.evmodder.HorseOwners.HorseManager;

public class SpawnListener implements Listener{
	HorseManager plugin;
	public SpawnListener(){
		plugin = HorseManager.getPlugin();
	}

	// Used to rank/record stats for wild horses
	@EventHandler
	public void onEntitySpawn(CreatureSpawnEvent evt){
		if(plugin.isClaimableHorseType(evt.getEntity())) plugin.updateData(evt.getEntity());
	}
}