package net.evmodder.HorseOwners.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import net.evmodder.HorseOwners.HorseManager;

public class PlayerLogoutListener implements Listener{
	@EventHandler
	public void onTame(PlayerQuitEvent evt){
		if(evt.getPlayer().isInsideVehicle() && HorseManager.getPlugin().getAPI().isClaimableHorseType(evt.getPlayer().getVehicle())){
			HorseManager.getPlugin().getAPI().updateDatabase(evt.getPlayer().getVehicle());
		}
	}
}