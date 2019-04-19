package net.evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.Horse.Variant;
import net.evmodder.HorseOwners.HorseLibrary;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
public class CommandSpawnHorse extends HorseCommand{
	final HashMap<String, List<String>> flags;

	public CommandSpawnHorse(){
		flags = new HashMap<String, List<String>>();
		flags.put("name:", null);
		flags.put("speed:", null);
		flags.put("jump:", null);
		flags.put("health:", null);
		flags.put("color:", Arrays.stream(Color.values()).map(c -> "color:"+c).collect(Collectors.toList()));
		flags.put("variant:", Arrays.stream(Variant.values()).map(v -> "variant:"+v).collect(Collectors.toList()));
		flags.put("style:", Arrays.stream(Style.values()).map(s -> "style:"+s).collect(Collectors.toList()));
		flags.put("tamed:", Arrays.asList("true", "false"));
	}

	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length > 0){
			HashSet<String> setFlags = new HashSet<String>();
			String lastArg = args[args.length-1];
			for(String arg : args){
				int fi = arg.indexOf(':');
				if(fi != -1 && fi != arg.length()-1) setFlags.add(arg.substring(0, fi+1).toLowerCase());
				else if(!arg.equals(lastArg)) return null;
			}
			lastArg = lastArg.toLowerCase();
			final List<String> tabCompletes = new ArrayList<String>();
			for(String flag : flags.keySet()){
				if(!setFlags.contains(flag) && flag.startsWith(lastArg)){
					if(lastArg.equals(flag)) return flags.get(flag);
					else tabCompletes.add(flag);
				}
			}
			return tabCompletes;
		}
		return null;
	}

	@Override public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	usage: /hm spawn [n:name] [s:speed] [j:jump] [h:health] [c:color] [v:variant] [t:style] [tamed]
		//hm spawn n:fat c:white t:white s:50 j:20 h:60
		if(sender instanceof Player == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players");
			return true;
		}

		String name = null;
		EntityType variant = EntityType.HORSE;
		Color color = null;
		Style style = null;
		double jump=0, speed=0, health=0;
		boolean tamed = false;

		for(String input : args){
			String arg = input.toUpperCase();
			int sep = arg.indexOf(':');
			if(sep == -1){
				if(!(tamed = arg.replace("-", "").toLowerCase().equals("tamed"))){
					sender.sendMessage(ChatColor.RED+"Invalid arguments (perhaps missing ':'?)");
					return false;
				}
			}
			String postSep = arg.substring(sep+1);
			if(arg.startsWith("N")) name = postSep;
			else if(arg.startsWith("V") || arg.startsWith("TYPE:")){
				try{variant = EntityType.valueOf(postSep);}
				catch(IllegalArgumentException ex){
					sender.sendMessage(ChatColor.RED+"Invalid variant \""+ChatColor.GRAY+postSep
						+ChatColor.RED+"\"\n"+ChatColor.GRAY+"Possible values: "+Arrays.asList(Variant.values()));
					return true;
				}
			}
			else if(arg.startsWith("C")){
				try{color = Color.valueOf(postSep);}
				catch(IllegalArgumentException ex){
					sender.sendMessage(ChatColor.RED+"Invalid color \""+ChatColor.GRAY+postSep
							+ChatColor.RED+"\"\n"+ChatColor.GRAY+"Possible values: "+Arrays.asList(Color.values()));
					return true;
				}
			}
			else if(arg.startsWith("T:") || arg.startsWith("STYLE:")){
				try{style = Style.valueOf(postSep);}
				catch(IllegalArgumentException ex){
					sender.sendMessage(ChatColor.RED+"Invalid style \""+ChatColor.GRAY+postSep
							+ChatColor.RED+"\"\n"+ChatColor.GRAY+"Possible values: "+Arrays.asList(Style.values()));
					return true;
				}
			}
			else if(arg.startsWith("J")){
				try{jump = Double.parseDouble(postSep);}
				catch(NumberFormatException ex){
					sender.sendMessage(ChatColor.RED+"Invalid jump, only accepts number values");
					return true;
				}
			}
			else if(arg.startsWith("S")){
				try{speed = Double.parseDouble(postSep);}
				catch(NumberFormatException ex){
					sender.sendMessage(ChatColor.RED+"Invalid speed, only accepts number values");
					return true;
				}
			}
			else if(arg.startsWith("H")){
				try{health = Double.parseDouble(postSep);}
				catch(NumberFormatException ex){
					sender.sendMessage(ChatColor.RED+"Invalid health, only accepts number values");
					return true;
				}
			}
			else if(arg.startsWith("TAME")) tamed = postSep.equals("TRUE") || postSep.equals("YES");
		}

		Player p = (Player) sender;
		AbstractHorse horse = (AbstractHorse) p.getWorld().spawnEntity(p.getLocation(), variant);

		if(name != null) horse.setCustomName(name);
		if(variant == EntityType.HORSE){
			if(color != null) ((Horse)horse).setColor(color);
			if(style != null) ((Horse)horse).setStyle(style);
		}
		if(jump != 0) horse.setJumpStrength(HorseLibrary.denormalizeJump(jump));
		if(speed != 0) HorseLibrary.speedCalc.setHorseSpeed(horse, HorseLibrary.denormalizeSpeed(speed));
		if(health != 0) HorseLibrary.setMaxHealth(horse, health);
		if(tamed) horse.setTamed(true);

		sender.sendMessage(ChatColor.GREEN+"Successfully spawned your horse!");
		return true;
	}
}