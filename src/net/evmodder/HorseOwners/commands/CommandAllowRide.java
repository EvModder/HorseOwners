package net.evmodder.HorseOwners.commands;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import net.evmodder.HorseOwners.HorseUtils;

public class CommandAllowRide extends HorseCommand{
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length == 1){
			args[0] = args[0].toLowerCase();
			return plugin.getServer().getOnlinePlayers().stream().map(p -> p.getName())
					.filter(name -> name.toLowerCase().startsWith(args[0])).limit(20).collect(Collectors.toList());
		}
		else if(args.length > 1 && plugin.getServer().getPlayer(args[0]) != null){
			final String arg = HorseUtils.cleanName(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
			return (sender instanceof Player
					? plugin.getAPI().getHorses(((Player)sender).getUniqueId())
					: plugin.getAPI().getAllClaimedHorses()
					).stream().filter(name -> name.startsWith(arg)).limit(20).collect(Collectors.toList());
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
		String cleanHorseName;
		if(args.length > 1) cleanHorseName = HorseUtils.cleanName(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
		else if(p != null && p.isInsideVehicle() && p.getVehicle() instanceof Horse){
			cleanHorseName = HorseUtils.cleanName(p.getVehicle().getCustomName());
			if(cleanHorseName == null || plugin.getAPI().isClaimedHorse(cleanHorseName) == false){
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

		if(!plugin.getAPI().horseExists(cleanHorseName)){
			sender.sendMessage(ChatColor.RED+"Unknown horse! (check spelling)"+ChatColor.GRAY+'\n'+command.getUsage());
			COMMAND_SUCCESS = false;
			return true;
		}
		if(plugin.getAPI().isClaimedHorse(cleanHorseName) == false){
			sender.sendMessage(ChatColor.GRAY+"This horse is ownerless, anyone can ride it!");
			COMMAND_SUCCESS = false;
			return true;
		}
		if(p != null && plugin.getAPI().canAccess(p.getUniqueId(), cleanHorseName) == false){
			sender.sendMessage(ChatColor.RED+"You do not own this horse");
			COMMAND_SUCCESS = false;
			return true;
		}

		//Yay got to here! Now give them a boost up!
		plugin.getAPI().grantOneTimeAccess(recipient.getUniqueId(), cleanHorseName);

		//Tell the sender
		final String rawHorseName = plugin.getAPI().getHorseName(cleanHorseName);
		sender.sendMessage(ChatColor.GREEN+"Player "+ChatColor.GRAY+args[0]
					+ChatColor.GREEN+" is now allowed one ride on the horse, "
					+ChatColor.GRAY+rawHorseName+ChatColor.GREEN+'.');
		
		//Tell the allowed rider
		if(recipient.isOnline()) recipient.getPlayer()
				.sendMessage(ChatColor.GRAY+sender.getName()+ChatColor.GREEN
				+" has granted you permission for a single ride on their horse, "
				+ChatColor.GRAY+rawHorseName+ChatColor.GREEN+'.');
		COMMAND_SUCCESS = true;
		return true;
	}
}