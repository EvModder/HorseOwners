package Evil_Code_HorseOwners.commands;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandUnleashHorse extends HorseCommand{
	
	@Override
	public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm unleash [horse]
		Player p = (sender instanceof Player) ? (Player)sender : null;
		AbstractHorse h;
		if(p != null && p.isInsideVehicle() && p.getVehicle() instanceof AbstractHorse){
			h = (AbstractHorse) p.getVehicle();
		}
		else{
			if(args.length == 0){
				sender.sendMessage("§cToo few arguments!");
				return false;
			}
			String horseName = StringUtils.join(args, ' ');
			
			if(plugin.isClaimedHorse(horseName) == false){
				sender.sendMessage("§cUnknown horse (check name spelling)");
	//			sender.sendMessage("§cUnclaimed horses cannot be teleported via command, you must first use /claimhorse");
				return false;
			}
			if(p != null && plugin.canAccess(p, horseName) == false){
				sender.sendMessage("§cYou cannot unleash horses which you do not own");
				return true;
			}
			Entity e = plugin.findClaimedHorse(horseName, null);
			if(e == null || !(e instanceof AbstractHorse)){
				sender.sendMessage("§cUnable to find specified horse! Perhaps the chunk it is in was unloaded?");
				return true;
			}
			h = (AbstractHorse) e;
		}
		if(!h.isLeashed()){
			p.sendMessage("§7This horse is already unleashed");
		}
		//Yay got to here! Now set it freeee!
		else{
			if(h.getLeashHolder() instanceof LeashHitch){
				boolean noOtherLeashedMobs = true;
				for(Entity entity : h.getLeashHolder().getNearbyEntities(8, 8, 8)){
					if(entity instanceof LivingEntity && ((LivingEntity)entity).isLeashed()
							&& entity.getUniqueId().equals(h.getUniqueId()) == false
							&& ((LivingEntity)entity).getLeashHolder().getUniqueId().equals(h.getLeashHolder().getUniqueId()))
					{
						noOtherLeashedMobs = false;
					}
				}
				if(noOtherLeashedMobs) ((LeashHitch)h.getLeashHolder()).remove();
			}
			h.setLeashHolder(null);
			h.getWorld().dropItem(h.getLocation(), new ItemStack(Material.LEAD));
			p.sendMessage("§aHorse unleashed!");
		}
		return true;
	}
}