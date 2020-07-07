package net.evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class CommandRenameHorse extends HorseCommand{
	final CommandClaimHorse claimHorseCommandExecutor;

	public CommandRenameHorse(){
		claimHorseCommandExecutor = (CommandClaimHorse)plugin.getCommand("claimhorse").getExecutor();
	}

	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length > 0){
			String arg = String.join(" ", args).toLowerCase();
			final List<String> tabCompletes = new ArrayList<String>();
			byte shown = 0;
			for(String horseName : sender instanceof Player
						? plugin.getHorseOwners().getOrDefault(((Player)sender).getUniqueId(), new HashSet<>())
						: plugin.getAllHorses()){
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
		//cmd:	/hm rename [name]
		if(sender instanceof Player){
			Player p = (Player)sender;
			if(p.isInsideVehicle() && plugin.isClaimableHorseType(p.getVehicle())){
				return claimHorseCommandExecutor.onHorseCommand(sender, command, label, args);
			}
		}
		//cmd:	/hm rename <oldName> <newName>
		if(!sender.hasPermission("horseowners.rename.remote")){
			sender.sendMessage(ChatColor.RED+"You are missing the permission node to rename horses remotely");
			COMMAND_SUCCESS = false;
			return true;
		}
		if(args.length < 2){
			sender.sendMessage(ChatColor.RED+"Too few arguments!"+ChatColor.GRAY+" (Please supply an old and a new name)");
			COMMAND_SUCCESS = false;
			return false;
		}
		String anyHorse = null, claimedHorse = null, accessHorse = null;
		String newName = null;
		for(int i=1; i<args.length; ++i){
			String candidate = String.join(" ", Arrays.copyOfRange(args, 0, i));
			if(plugin.horseExists(candidate)){
				anyHorse = candidate;
				newName = String.join(" ", Arrays.copyOfRange(args, i, args.length));
				if(plugin.isClaimedHorse(candidate)){
					claimedHorse = candidate;
					if(sender instanceof Player && plugin.canAccess((Player)sender, candidate)){
						accessHorse = candidate;
						break;
					}
				}
			}
		}
		if(anyHorse == null){
			sender.sendMessage(ChatColor.RED+"Unknown horse '"
					+ChatColor.GRAY+args[0]
					+ChatColor.DARK_GRAY+"[ "+String.join(" ", Arrays.copyOfRange(args, 1, args.length-1))+"]"
					+ChatColor.RED+"'");
			COMMAND_SUCCESS = false;
			return false;
		}
		if(claimedHorse == null && !sender.hasPermission("horseowners.rename.unclaimed")){
			sender.sendMessage(ChatColor.RED+"You cannot rename wild horse '"+ChatColor.GRAY+anyHorse+ChatColor.RED+"'");
			COMMAND_SUCCESS = false;
			return true;
		}
		if(accessHorse == null && !sender.hasPermission("horseowners.rename.others")){
			sender.sendMessage(ChatColor.RED+"You cannot rename a horse which you do not own!");
			COMMAND_SUCCESS = false;
			return true;
		}
		String oldName = accessHorse != null ? accessHorse : claimedHorse != null ? claimedHorse : anyHorse;

		Entity horse = plugin.findClaimedHorse(oldName);
		if(horse == null){
			sender.sendMessage(ChatColor.RED+"Unable to find horse! Perhaps the chunk it was in is unloaded?");
			COMMAND_SUCCESS = false;
			return true;
		}

		switch(claimHorseCommandExecutor.attemptNameHorse(sender, horse, newName)){
			case FAILED: COMMAND_SUCCESS = false; return true;
			case FAILED_HINT: COMMAND_SUCCESS = false; return false;
			case RENAMED: COMMAND_SUCCESS = true; return true; // Horsie gets a new name!
			case NAMED: default:
				plugin.getLogger().severe("Strange return result in CommandRenameHorse!");
				sender.sendMessage(ChatColor.RED+"An unknown error occured. Please contact administrators");
				return false;
		}
	}
}