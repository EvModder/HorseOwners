package net.evmodder.HorseOwners.commands;

import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.ItemStack;
import net.evmodder.EvLib.extras.TextUtils;
import net.evmodder.HorseOwners.HorseLibrary;

public class CommandClaimHorse extends HorseCommand{
	final boolean renameNametag, alphanumeric;
	final int minNameLength, maxNameLength;

	public CommandClaimHorse(){
		renameNametag = plugin.getConfig().getBoolean("nametag-required-to-rename");
		alphanumeric = plugin.getConfig().getBoolean("names-mostly-alphanumeric");

		minNameLength = plugin.getConfig().getInt("min-name-length");
		maxNameLength = plugin.getConfig().getInt("max-name-length");
		if(maxNameLength < minNameLength) plugin.getLogger().severe(
				"max-name-length("+maxNameLength+") is < than " +
				"min-name-length("+minNameLength+")!");
	}

	@Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a){return null;}

	enum RenameResult {FAILED, FAILED_HINT, RENAMED, NAMED};
	public RenameResult attemptNameHorse(CommandSender sender, Entity horse, String newName){
		if(alphanumeric) newName = newName.replaceAll("[:(),"+ChatColor.COLOR_CHAR+"<>{}\\-\\[\\]\\.'\"]", "");
		if(sender.hasPermission("horseowners.coloredname")){
			if(sender.hasPermission("horseowners.fullformats") == false){
				newName = newName.replace("&k", "").replace("&m", "").replace("&n", "");
			}
			newName = TextUtils.translateAlternateColorCodes('&', newName);
		}
		newName.replaceAll("\\s{2,}", " ").trim();//remove leftover spaces
		int newNameLength = Math.max(ChatColor.stripColor(newName).length(), TextUtils.strLen(newName, false)/6);

		String oldName = horse.getCustomName();
		if(oldName != null){
			if(newName.equals(oldName)){
				sender.sendMessage(ChatColor.RED+"This horse already has that name!");
				return RenameResult.FAILED;
			}
			if(plugin.isLockedHorse(oldName)){
				sender.sendMessage(ChatColor.RED+"You cannot rename "
							+ChatColor.GRAY+ChatColor.ITALIC+oldName+ChatColor.RED+'.');
				return RenameResult.FAILED_HINT;
			}
		}
		if(newNameLength < minNameLength){
			sender.sendMessage(ChatColor.RED+"Too short of a name!");
			return RenameResult.FAILED;
		}
		if(newNameLength > maxNameLength){
			sender.sendMessage(ChatColor.RED+"Too long of a name!");
			return RenameResult.FAILED;
		}
		plugin.getLogger().info(("old name: "+oldName));
		plugin.getLogger().info(("new name: "+newName));
		if(plugin.horseExists(newName) && (oldName == null ||
				!HorseLibrary.cleanName(oldName).equals(HorseLibrary.cleanName(newName)))){
			sender.sendMessage(ChatColor.RED+"That name has already been taken!");
			return RenameResult.FAILED;
		}
		if((oldName == null || oldName.equals(newName) == false)
				&& renameNametag && sender instanceof Player && ((Player)sender).getGameMode() != GameMode.CREATIVE){
			if(((Player)sender).getInventory().contains(Material.NAME_TAG) == false){
				sender.sendMessage(ChatColor.RED+"You need a nametag in order to "
					+(oldName == null ? "name/claim" : "rename")+" a horse!");
				return RenameResult.FAILED;
			}
			//charge 1 nametag
			Player p = (Player)sender;
			ItemStack firstNametag = p.getInventory().getItem(p.getInventory().first(Material.NAME_TAG));
			firstNametag.setAmount(firstNametag.getAmount()-1);
			p.getInventory().setItem(p.getInventory().first(Material.NAME_TAG),
									((firstNametag.getAmount() > 0) ? firstNametag : new ItemStack(Material.AIR)));
		}

		if(oldName != null && oldName.equals(newName) == false){//if had a name previously
			boolean needToClaimFirst = (sender instanceof Player && !plugin.isOwner(((Player)sender).getUniqueId(), oldName));
			if(needToClaimFirst && plugin.addClaimedHorse(((Player)sender).getUniqueId(), horse) == false){
				sender.sendMessage(ChatColor.RED+"HorseClaimEvent cancelled by a plugin");
				COMMAND_SUCCESS = false;
				return RenameResult.FAILED_HINT;
			}
			if(plugin.renameHorse(oldName, newName) == false){
				sender.sendMessage(ChatColor.RED+"HorseRenameEvent cancelled by a plugin");
				return RenameResult.FAILED_HINT;
			}

			if(needToClaimFirst) sender.sendMessage(ChatColor.GREEN+"Successfully claimed "+ ChatColor.GRAY + ChatColor.ITALIC + oldName
					+ ChatColor.GREEN + " as your horse!");
			sender.sendMessage(ChatColor.GREEN+"Successfully renamed " + ChatColor.GRAY + ChatColor.ITALIC + oldName
					+ ChatColor.GREEN + " to " + ChatColor.GRAY + ChatColor.ITALIC + newName + ChatColor.GREEN + "!");
			horse.setCustomName(newName);//Change name
			return RenameResult.RENAMED;
		}
		horse.setCustomName(newName);//Set name
		return RenameResult.NAMED;
	}

	@Override
	public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm claim [name]
		if(sender instanceof Player == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players");
			COMMAND_SUCCESS = false;
			return true;
		}

		Player p = (Player)sender;
		if(!p.isInsideVehicle() || !plugin.isClaimableHorseType(p.getVehicle())){
			p.sendMessage(ChatColor.RED+"You must be riding on a horse to use this command");
			COMMAND_SUCCESS = false;
			return true;
		}
		Entity h = p.getVehicle();

		if(args.length == 0 && h.getCustomName() == null){
			sender.sendMessage(ChatColor.RED+"Too few arguments!"+ChatColor.GRAY+" (Please supply a name)");
			COMMAND_SUCCESS = false;
			return false;
		}

		if(h instanceof Tameable && ((Tameable)h).isTamed() == false){
			p.sendMessage(ChatColor.RED+"You must first tame this horse before you can claim it");
			COMMAND_SUCCESS = false;
			return true;
		}

		String oldName = h.getCustomName();
		boolean isOwner = plugin.isOwner(p.getUniqueId(), oldName);

		if(oldName != null && plugin.isClaimedHorse(oldName) && isOwner == false){
			p.sendMessage(ChatColor.RED+"This horse already belongs to someone!");
			COMMAND_SUCCESS = false;
			return true;
		}

		if(args.length == 0){
			if(oldName == null){
				p.sendMessage(ChatColor.RED+"Please supply a name for this horse\n"+ChatColor.GRAY+command.getUsage());
				COMMAND_SUCCESS = false;
				return false;
			}
			if(isOwner){
				p.sendMessage(ChatColor.GRAY+"You already own this horse");
				COMMAND_SUCCESS = false;
				return true;
			}
		}
		else{
			String newName = StringUtils.join(args, ' ');
			switch(attemptNameHorse(sender, h, newName)){
				case FAILED: COMMAND_SUCCESS = false; return true;
				case FAILED_HINT: COMMAND_SUCCESS = false; return false;
				case RENAMED: COMMAND_SUCCESS = true; return true;
				case NAMED: default: break;
			}
		}

		//My horsie!
		if(plugin.addClaimedHorse(p.getUniqueId(), h) == false){
			p.sendMessage(ChatColor.RED+"HorseClaimEvent cancelled by a plugin");
			COMMAND_SUCCESS = false;
			return true;
		}
		sender.sendMessage(ChatColor.GREEN+"Successfully claimed " + ChatColor.GRAY + ChatColor.ITALIC
					+ h.getCustomName() + ChatColor.GREEN + " as your horse!");
		COMMAND_SUCCESS = true;
		return true;
	}
}