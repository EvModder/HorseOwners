package net.evmodder.HorseOwners.listeners;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class DamageListener implements Listener{
	
	//Reduce fall damage
	@EventHandler
	public void onEntityDamage(EntityDamageEvent evt){
		if(evt.getCause() == DamageCause.FALL){
			if(evt.getEntity() instanceof AbstractHorse){
				double damage = evt.getDamage() - 5;
				if(damage < 0)evt.setCancelled(true);
				else evt.setDamage(damage);
			}
			else if(evt.getEntity() instanceof Player){
				Player p = (Player) evt.getEntity();
				if(p.isInsideVehicle())if(p.getVehicle() instanceof AbstractHorse){
					
					double damage = evt.getDamage()/1.5 - 2;
					if(damage < 0)evt.setCancelled(true);
					else evt.setDamage(damage);
				}
			}
		}//if FALL_DAMAGE
	}
}
