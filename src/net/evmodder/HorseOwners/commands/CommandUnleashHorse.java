package net.evmodder.HorseOwners.commands;

import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.evmodder.HorseOwners.HorseUtils;

public class CommandUnleashHorse extends HorseCommand{
	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length > 0){
			final String arg = HorseUtils.cleanName(String.join(" ", args));
			return (sender instanceof Player
					? plugin.getAPI().getHorses(((Player)sender).getUniqueId())
					: plugin.getAPI().getAllHorses()
					).stream().filter(name -> name.startsWith(arg)).limit(20).collect(Collectors.toList());
		}
		return null;
	}

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
				COMMAND_SUCCESS = false;
				return false;
			}
			String cleanHorseName = HorseUtils.cleanName(String.join(" ", args));
			
			if(plugin.getAPI().isClaimedHorse(cleanHorseName) == false){
				sender.sendMessage("§cUnknown horse (check name spelling)");
	//			sender.sendMessage("§cUnclaimed horses cannot be teleported via command, you must first use /claimhorse");
				COMMAND_SUCCESS = false;
				return false;
			}
			if(p != null && plugin.getAPI().canAccess(p.getUniqueId(), cleanHorseName) == false){
				sender.sendMessage("§cYou cannot unleash horses which you do not own");
				COMMAND_SUCCESS = false;
				return true;
			}
			Entity e = plugin.getAPI().getHorse(cleanHorseName, /*loadChunk/*/true);
			if(e == null || !(e instanceof AbstractHorse)){
				sender.sendMessage("§cUnable to find specified horse! Perhaps the chunk it was in is unloaded?");
				COMMAND_SUCCESS = false;
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
		COMMAND_SUCCESS = true;
		return true;
	}
}