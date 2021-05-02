package net.evmodder.HorseOwners.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.spigotmc.event.entity.EntityDismountEvent;
import net.evmodder.HorseOwners.HorseManager;

public class DismountListener implements Listener{
	private HorseManager plugin;

	public DismountListener(){
		plugin = HorseManager.getPlugin();
	}

	@EventHandler
	public void onHorseDismount(EntityDismountEvent evt){
		if(plugin.getAPI().isClaimableHorseType(evt.getDismounted())){
			new BukkitRunnable(){@Override public void run(){plugin.getAPI().updateDatabase(evt.getDismounted());}}.runTaskLater(plugin, 1);
		}
	}
}