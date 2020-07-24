package net.evmodder.HorseOwners.commands;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import net.evmodder.HorseOwners.EditableHorseAttributesUtil;
import net.evmodder.HorseOwners.EditableHorseAttributesUtil.HorseAttributes;
import org.bukkit.entity.Player;

public class CommandSpawnHorse extends HorseCommand{
	final EditableHorseAttributesUtil editableAttributesUtil;

	public CommandSpawnHorse(){
		editableAttributesUtil = new EditableHorseAttributesUtil(plugin);
	}

	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(sender instanceof Player == false) return null;
		return editableAttributesUtil.getTabCompletions(sender, args);
	}

	@Override public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	usage: /hm spawn [n:name] [s:speed] [j:jump] [h:health] [c:color] [v:variant] [t:style] [o:owner] [r:tamer] [i:strength]
		//hm spawn n:fat c:white t:white s:50 j:20 h:60
		if(sender instanceof Player == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players");
			COMMAND_SUCCESS = false;
			return true;
		}

		HorseAttributes attributes = editableAttributesUtil.parseAttributes(sender, args);
		if(attributes.success == false){
			COMMAND_SUCCESS = false;
			return !attributes.parseError;
		}

		Player p = (Player) sender;
		Entity horse = p.getWorld().spawnEntity(p.getLocation(), attributes.variant);
		if(horse == null){
			sender.sendMessage(ChatColor.RED+"Failed to spawn horse (blocked by plugin or world settings)");
			COMMAND_SUCCESS = false;
			return true;
		}
		editableAttributesUtil.applyAttributes(horse, attributes);

		sender.sendMessage(ChatColor.GREEN+"Successfully spawned your horse!");
		COMMAND_SUCCESS = true;
		return true;
	}
}