package net.evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;

public class CommandAllowRide extends HorseCommand{
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length == 1){
			final List<String> tabCompletes = new ArrayList<String>();
			args[0] = args[0].toLowerCase();
			for(Player p : plugin.getServer().getOnlinePlayers()){
				if(p.getName().startsWith(args[0])){
					tabCompletes.add(p.getName());
					if(tabCompletes.size() == 20) break;
				}
			}
			return tabCompletes;
		}
		else if(args.length > 1 && plugin.getServer().getPlayer(args[0]) != null){
			String arg = String.join(" ", args).toLowerCase();
			arg = arg.substring(arg.indexOf(' ')+1);
			final List<String> tabCompletes = new ArrayList<String>();
			byte shown = 0;
			for(String horseName : sender instanceof Player ?
					plugin.getHorseOwners().get(((Player)sender).getUniqueId()) : plugin.getAllClaimedHorses()){
				if(horseName.startsWith(arg)){
					tabCompletes.add(horseName);
					if(++shown == 20) break;
				}
			}
			return tabCompletes;
		}
		return null;
	}

	@Override @SuppressWarnings("deprecation")
	public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm allowride <Player> [horse]

		if(args.length == 0){
			sender.sendMessage(ChatColor.RED+"Too few arguments!"+ChatColor.GRAY+'\n'+command.getUsage());
			COMMAND_SUCCESS = false;
			return false;
		}

		OfflinePlayer recipient = sender.getServer().getOfflinePlayer(args[0]);
		if(recipient == null || recipient.hasPlayedBefore() == false){
			sender.sendMessage(ChatColor.RED+"Player \""+ChatColor.GOLD+args[0]
					+ChatColor.RED+"\" not found!"+ChatColor.GRAY+'\n'+command.getUsage());
			COMMAND_SUCCESS = false;
			return false;
		}

		Player p = (sender instanceof Player) ? (Player)sender : null;
		String horseName;
		if(args.length > 1) horseName = StringUtils.join(args, ' ', 1, args.length);
		else if(p != null && p.isInsideVehicle() && p.getVehicle() instanceof Horse){
			if((horseName=p.getVehicle().getCustomName()) == null || plugin.isClaimedHorse(horseName) == false){
				sender.sendMessage(ChatColor.GRAY+"This horse is ownerless, anyone can ride it!");
				COMMAND_SUCCESS = false;
				return true;
			}
		}
		else{
			sender.sendMessage(ChatColor.RED+"Please specify both a horse and a player"
						+ChatColor.GRAY+'\n'+command.getUsage());
			COMMAND_SUCCESS = false;
			return false;
		}

		if(!plugin.horseExists(horseName)){
			sender.sendMessage(ChatColor.RED+"Unknown horse! (check spelling)"+ChatColor.GRAY+'\n'+command.getUsage());
			COMMAND_SUCCESS = false;
			return true;
		}
		if(plugin.isClaimedHorse(horseName) == false){
			sender.sendMessage(ChatColor.GRAY+"This horse is ownerless, anyone can ride it!");
			COMMAND_SUCCESS = false;
			return true;
		}
		if(p != null && plugin.canAccess(p, horseName) == false){
			sender.sendMessage(ChatColor.RED+"You do not own this horse");
			COMMAND_SUCCESS = false;
			return true;
		}

		//Yay got to here! Now give them a boost up!
		plugin.grantOneTimeAccess(recipient.getUniqueId(), horseName);

		//Tell the sender
		sender.sendMessage(ChatColor.GREEN+"Player "+ChatColor.GRAY+args[0]
					+ChatColor.GREEN+" is now allowed one ride on the horse, "
					+ChatColor.GRAY+horseName+ChatColor.GREEN+'.');
		
		//Tell the allowed rider
		if(recipient.isOnline()) recipient.getPlayer()
				.sendMessage(ChatColor.GRAY+sender.getName()+ChatColor.GREEN
				+" has granted you permission for a single ride on their horse, "
				+ChatColor.GRAY+horseName+ChatColor.GREEN+'.');
		COMMAND_SUCCESS = true;
		return true;
	}
}