package net.evmodder.HorseOwners.commands;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import net.evmodder.HorseOwners.EditableHorseAttributesUtil;
import net.evmodder.HorseOwners.EditableHorseAttributesUtil.HorseAttributes;
import org.bukkit.entity.Player;

public class CommandEditHorse extends HorseCommand{
	final EditableHorseAttributesUtil editableAttributesUtil;

	public CommandEditHorse(){
		editableAttributesUtil = new EditableHorseAttributesUtil(plugin);
	}

	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(sender instanceof Player == false || !((Player)sender).isInsideVehicle()) return null;
		return editableAttributesUtil.getTabCompletions(sender, args);
	}

	@Override public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	usage: /hm edit [n:name] [s:speed] [j:jump] [h:health] [c:color] [v:variant] [t:style] [o:owner] [r:tamer] [baby:]
		//hm edit n:fat c:white t:white s:12 j:4 h:25 r:EvModder baby:true
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
		Entity horse = p.getVehicle();

		HorseAttributes attributes = editableAttributesUtil.parseAttributes(sender, args);
		if(attributes.success == false){
			COMMAND_SUCCESS = false;
			return !attributes.parseError;
		}
		editableAttributesUtil.applyAttributes(horse, attributes);

		sender.sendMessage(ChatColor.GREEN+"Horse modification completed successfully");
		COMMAND_SUCCESS = true;
		return true;
	}
}