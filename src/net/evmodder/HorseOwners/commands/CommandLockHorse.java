package net.evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.evmodder.HorseOwners.HorseUtils;

public class CommandLockHorse extends HorseCommand{
	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length > 0){
			String arg = String.join(" ", args).toLowerCase();
			final List<String> tabCompletes = new ArrayList<String>();
			byte shown = 0;
			for(String horseName : sender instanceof Player
						? plugin.getAPI().getHorses(((Player)sender).getUniqueId())
						: plugin.getAPI().getAllHorses()){
				if(horseName.startsWith(arg)){
					tabCompletes.add(horseName);
					if(++shown == 20) break;
				}
			}
			return tabCompletes;
		}
		return null;
	}

	@Override
	public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm lock [-name]

		Player p = (sender instanceof Player) ? (Player)sender : null;
		String horseName;

		if(p != null && p.isInsideVehicle() && plugin.getAPI().isClaimableHorseType(p.getVehicle())){
			if(p.getVehicle().getCustomName() == null){
				p.sendMessage(ChatColor.RED+"This horse is ownerless!\nTry claiming it first with /namehorse");
				COMMAND_SUCCESS = false;
				return true;
			}
			else{
				horseName = p.getVehicle().getCustomName();
			}
		}
		else if(args.length == 0){
			sender.sendMessage("§cPlease specify the horse you want to lock!§7\n"+command.getUsage());
			COMMAND_SUCCESS = false;
			return false;
		}
		else horseName = String.join(" ", args);

		//by this point, a name has been determined
		final String cleanHorseName = HorseUtils.cleanName(horseName);
		if(plugin.getAPI().isLockedHorse(cleanHorseName)){
			sender.sendMessage("§7This horse's name has already been locked");
			COMMAND_SUCCESS = false;
			return true;
		}
		if(!plugin.getAPI().horseExists(cleanHorseName)){
			sender.sendMessage("§cUnknown horse! (check spelling)§7\n"+command.getUsage());
			COMMAND_SUCCESS = false;
			return true;
		}
		if(plugin.getAPI().isClaimedHorse(cleanHorseName)){
			if(p != null && plugin.getAPI().canAccess(p.getUniqueId(), cleanHorseName) == false){
				sender.sendMessage("§cYou cannot lock horses which you do not own");
				COMMAND_SUCCESS = false;
				return true;
			}
		}
/*		//Check to confirm the horse's existence, even though we don't need the entity
		else if(plugin.findClaimedHorse(horseName, null) == null){
			sender.sendMessage("§cUnable to find your horse! Perhaps the chunk it is in was unloaded?");
			return true;
		}*/

		//Yay got to here! Now make it forever!
		sender.sendMessage("§7Your horse's name been locked! Nobody can change it now.");
		plugin.getAPI().setHorseNameLock(cleanHorseName, /*locked=*/true);
		COMMAND_SUCCESS = true;
		return true;
	}
}