package net.evmodder.HorseOwners.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;
import net.evmodder.HorseOwners.HorseManager;

public class TameListener implements Listener{

	@EventHandler
	public void onTame(EntityTameEvent evt){
		if(evt.getEntity().getCustomName() != null && HorseManager.getPlugin().isClaimableHorseType(evt.getEntity()))
		{
			Player p = evt.getEntity().getServer().getPlayer(evt.getOwner().getUniqueId());
			if(p != null) HorseManager.getPlugin().addClaimedHorse(p.getUniqueId(), evt.getEntity());
		}
	}
}