package Evil_Code_HorseOwners.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import Evil_Code_HorseOwners.HorseManager;

public class LeadBreakListener implements Listener{
	private HorseManager plugin;

	public LeadBreakListener(){
		plugin = HorseManager.getPlugin();
	}

	//Disable lead breaking
	@EventHandler
	public void onLeashPunch(HangingBreakByEntityEvent evt){
		if(evt.getEntity() instanceof LeashHitch && evt.getRemover() instanceof Player){
			LeashHitch leash = (LeashHitch) evt.getEntity();

			for(Entity e : leash.getNearbyEntities(15, 15, 15)){
				if(plugin.isClaimableHorseType(e) && e.getCustomName() != null && e instanceof LivingEntity){
					LivingEntity h = (LivingEntity) e;
					if(h.isLeashed() && h.getLeashHolder().getUniqueId().equals(leash.getUniqueId())){
						Player p = (Player) evt.getRemover();

						if(plugin.canAccess(p, h.getCustomName()) == false){
							evt.setCancelled(true);
							p.sendMessage(ChatColor.RED+"You do not have permission to unleash this horse.");
						}
					}//if the leash holder is the leash hitch that was punched
				}//if the entity is a horse
			}//loop through entities
		}//if entity is a leash-hitch and attacker is a player
	}
}