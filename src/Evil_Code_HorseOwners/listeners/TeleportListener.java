package Evil_Code_HorseOwners.listeners;

import org.bukkit.entity.Entity;
import java.util.UUID;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import Evil_Code_HorseOwners.HorseLibrary;
import Evil_Code_HorseOwners.HorseManager;
import com.onarandombox.MultiverseCore.MultiverseCore;

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
				MultiverseCore mv = (MultiverseCore) plugin.getServer().getPluginManager().getPlugin("Multiverse-Core");
				if(mv == null || mv.isEnabled() == false ||
					mv.getMVWorldManager().getMVWorld(evt.getFrom().getWorld()).getGameMode() != 
					mv.getMVWorldManager().getMVWorld(evt.getTo().getWorld()).getGameMode())
				{
					return;//Unable to teleport to a world with a different gamemode
				}
			}
			else{
				return;//Unable to teleport to a different world
			}
		}

		//Teleporting must be more than 20 blocks 
		if(HorseLibrary.notFar(evt.getFrom(), evt.getTo())) return;

		boolean safeForHorses = HorseLibrary.safeForHorses(evt.getTo());
		if(safeForHorses == false && leashedMobs == false) return;

		UUID pUUID = evt.getPlayer().getUniqueId();
		for(Entity e : evt.getPlayer().getNearbyEntities(6, 6, 6)){
			if(e instanceof LivingEntity){
				boolean isHorse = e instanceof AbstractHorse;
				if(isHorse && !safeForHorses) continue;
				LivingEntity le = (LivingEntity)e;
				UUID leashHolder = le.isLeashed() ? le.getLeashHolder().getUniqueId() : null;
				if(preventIfTied && leashHolder != null && !leashHolder.equals(pUUID)) continue;

				if(le.isLeashed() && leashHolder.equals(pUUID)){
					if(isHorse || (leashedMobs && (e.getCustomName() != null || !requireName))){
						HorseLibrary.teleportEntityWithPassengers(e, evt.getTo());
					}
				}
				else if(unleashedHorses && isHorse){
					boolean owned = le.getCustomName() != null && plugin.isOwner(pUUID, le.getCustomName());
					boolean nearby = evt.getPlayer().getNearbyEntities(4, 1, 4).contains(le);
					if(nearby && owned) HorseLibrary.teleportEntityWithPassengers(e, evt.getTo());
				}
			}
		}
	}//on teleport
}