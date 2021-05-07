package net.evmodder.HorseOwners.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.spigotmc.event.entity.EntityMountEvent;
import net.evmodder.HorseOwners.HorseManager;

public class MountListener implements Listener{
	private HorseManager plugin;

	public MountListener(){
		plugin = HorseManager.getPlugin();
	}

	@EventHandler
	public void onHorseMount(EntityMountEvent evt){
		if(plugin.getAPI().isClaimableHorseType(evt.getMount())){
			new BukkitRunnable(){@Override public void run(){
				plugin.getAPI().updateDatabase(evt.getMount());
			}}.runTaskLater(plugin, 1);
		}
	}
}