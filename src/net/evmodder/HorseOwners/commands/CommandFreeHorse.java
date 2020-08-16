package net.evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.evmodder.HorseOwners.HorseUtils;

public class CommandFreeHorse extends HorseCommand{
	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length > 0){
			String arg = String.join(" ", args).toLowerCase();
			final List<String> tabCompletes = new ArrayList<String>();
			byte shown = 0;
			for(String horseName : sender instanceof Player
					? plugin.getAPI().getHorses(((Player)sender).getUniqueId())
					: plugin.getAPI().getAllClaimedHorses()){
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
		//cmd:	/hm free [-name] [-full]
		Player p = (sender instanceof Player) ? (Player)sender : null;
		String horseName;
		if(args.length > 0) horseName = String.join(" ", args);
		else if(p != null && p.isInsideVehicle() && plugin.getAPI().isClaimableHorseType(p.getVehicle())){
			if(p.getVehicle().getCustomName() == null || !plugin.getAPI().isClaimedHorse(HorseUtils.cleanName(p.getVehicle().getCustomName()))){
				sender.sendMessage(ChatColor.GRAY+"This horse is already ownerless!");
				COMMAND_SUCCESS = false;
				return false;
			}
			else horseName = p.getVehicle().getCustomName();
		}
		else{
			sender.sendMessage(ChatColor.RED+"Please specify the horse you want to set free"
						+ChatColor.GRAY+'\n'+command.getUsage());
			COMMAND_SUCCESS = false;
			return false;
		}
		final String cleanHorseName = HorseUtils.cleanName(horseName);

		if(plugin.getAPI().isClaimedHorse(cleanHorseName) == false){
			sender.sendMessage(ChatColor.RED+"Unknown horse (check name spelling)"+ChatColor.GRAY+'\n'+command.getUsage());
			COMMAND_SUCCESS = false;
			return false;
		}
		if(p != null && plugin.getAPI().canAccess(p.getUniqueId(), cleanHorseName) == false){
			sender.sendMessage(ChatColor.RED+"You cannot manage horses which you do not own");
			COMMAND_SUCCESS = false;
			return true;
		}

		//Yay! now set it freeeee!
		boolean wasOwner = p != null && plugin.getAPI().isOwner(p.getUniqueId(), cleanHorseName);
		plugin.getAPI().unclaimHorse(cleanHorseName);
		if(wasOwner) sender.sendMessage(ChatColor.GREEN+"You have freed your horse!");
		else sender.sendMessage(ChatColor.GREEN+"You have freed the horse: "+ChatColor.GRAY+horseName+ChatColor.GREEN+'.');
		COMMAND_SUCCESS = true;
		return true;
	}
}