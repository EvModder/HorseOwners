package net.evmodder.HorseOwners.listeners;

import org.bukkit.entity.Entity;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import net.evmodder.EvLib.hooks.MultiverseHook;
import net.evmodder.HorseOwners.HorseUtils;
import net.evmodder.HorseOwners.HorseManager;

public class TeleportListener implements Listener{
	final boolean leashedMobs, requireName, unleashedHorses, preventIfTied;
	private HorseManager plugin;

	public TeleportListener(){
		plugin = HorseManager.getPlugin();
		leashedMobs = plugin.getConfig().getBoolean("teleport-leashed-mobs", true);
		requireName = plugin.getConfig().getBoolean("teleport-leashed-mobs-if-named", true);
		preventIfTied = plugin.getConfig().getBoolean("teleport-only-if-untied", true);
		unleashedHorses = plugin.getConfig().getBoolean("teleport-unleashed-owned-horses", false);
	}

	//Bring horses when teleporting
	@EventHandler(priority = EventPriority.MONITOR)
	public void onTp(PlayerTeleportEvent evt){
		if(evt.isCancelled()) return;

		if(evt.getFrom().getWorld().getName().equals(evt.getTo().getWorld().getName()) == false &&
				evt.getPlayer().hasPermission("evp.horseowners.tpansworld.*") == false){
			if(evt.getPlayer().hasPermission("evp.horseowners.tpansworld.samegamemode")){
				GameMode fromGM = MultiverseHook.getWorldGameMode(evt.getFrom().getWorld());
				GameMode toGM = MultiverseHook.getWorldGameMode(evt.getTo().getWorld());
				if(fromGM != toGM) return;//Unable to teleport to a world with a different gamemode
			}
			else{
				return;//Unable to teleport to a different world
			}
		}

		//Teleporting must be more than 20 blocks 
		if(HorseUtils.notFar(evt.getFrom(), evt.getTo())) return;

		boolean safeForHorses = HorseUtils.safeForHorses(evt.getTo());
		if(safeForHorses == false && leashedMobs == false) return;

		UUID pUUID = evt.getPlayer().getUniqueId();
		boolean failedLeashedHorseTp = false;
		for(Entity e : evt.getPlayer().getNearbyEntities(6, 6, 6)){
			if(e instanceof LivingEntity){
				boolean isHorse = e instanceof AbstractHorse;
				boolean canTp = false;
				LivingEntity le = (LivingEntity)e;
				UUID leashHolder = le.isLeashed() ? le.getLeashHolder().getUniqueId() : null;
				if(preventIfTied && leashHolder != null && !leashHolder.equals(pUUID)) continue;

				if(le.isLeashed() && leashHolder.equals(pUUID)){
					canTp = isHorse || (leashedMobs && (e.getCustomName() != null || !requireName));
					if(isHorse && !safeForHorses){
						failedLeashedHorseTp = true;
						continue;
					}
				}
				else if(unleashedHorses && isHorse){
					boolean owned = le.getCustomName() != null && plugin.getAPI().isOwner(pUUID, HorseUtils.cleanName(le.getCustomName()));
					boolean nearby = evt.getPlayer().getNearbyEntities(4, 1, 4).contains(le);
					canTp = nearby && owned;
				}
				if(canTp) HorseUtils.teleportEntityWithPassengers(e, evt.getTo());
			}
		}
		if(failedLeashedHorseTp) evt.getPlayer().sendMessage(ChatColor.GRAY
				+"Unable to bring horses since your destination was obstructed");
	}//on teleport
}