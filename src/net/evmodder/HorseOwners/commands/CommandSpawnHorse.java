package net.evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Style;
import net.evmodder.HorseOwners.HorseLibrary;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
public class CommandSpawnHorse extends HorseCommand{
	final HashMap<String, List<String>> flags;
	//final String[] variants = new String[]{
	//"HORSE", "DONKEY", "MULE", "LLAMA", "TRADER_LLAMA", "SKELETON_HORSE", "ZOMBIE_HORSE"};
	final Set<EntityType> claimableTypes;

	public CommandSpawnHorse(){
		claimableTypes = plugin.getClaimableHorseTypes();
		flags = new HashMap<String, List<String>>();
		flags.put("name:", null);
		flags.put("speed:", null);
		flags.put("jump:", null);
		flags.put("health:", null);
		flags.put("owner:", null);
		flags.put("tamer:", null);
		flags.put("color:", Arrays.stream(Color.values()).map(c -> "color:"+c).collect(Collectors.toList()));
		//flags.put("variant:", Arrays.stream(Variant.values()).map(v -> "variant:"+v).collect(Collectors.toList()));
		flags.put("variant", claimableTypes.stream().map(v -> "variant:"+v).collect(Collectors.toList()));
		flags.put("style:", Arrays.stream(Style.values()).map(s -> "style:"+s).collect(Collectors.toList()));
		flags.put("baby:", Arrays.asList("baby:true", "baby:false"));
	}

	List<String> getFlagCompletions(String flag, String arg){
		final List<String> tabCompletes = new ArrayList<String>();
		final List<String> flagVals = flags.get(flag);
		if(flagVals != null) for(String flagVal : flagVals){
			if(flagVal.startsWith(arg)) tabCompletes.add(flagVal);
		}
		else if(flag.equals("tamer:") || flag.equals("owner:")){
			tabCompletes.add(flag);
			int fi = arg.indexOf(':');
			arg = fi == -1 ? "" : arg.substring(fi+1);
			for(Player p : plugin.getServer().getOnlinePlayers()){
				if(p.getName().toLowerCase().startsWith(arg)) tabCompletes.add(flag+p.getName());
			}
		}
		else tabCompletes.add(flag);
		return tabCompletes;
	}
	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length > 0){
			HashSet<String> setFlags = new HashSet<String>();
			String lastArg = args[args.length-1];
			for(int i=0; i<args.length-1; ++i){
				int fi = args[i].indexOf(':');
				if(fi != -1 && fi != args[i].length()-1) setFlags.add(args[i].substring(0, fi+1).toLowerCase());
				else return null;
			}
			lastArg = lastArg.toLowerCase();
			final List<String> tabCompletes = new ArrayList<String>();
			boolean oneMatchingFlag = false;
			String matchingFlag = null;
			for(String flag : flags.keySet()){
				if(!setFlags.contains(flag)){
					if(lastArg.startsWith(flag)){
						return getFlagCompletions(flag, lastArg);
					}
					else if(flag.startsWith(lastArg)){
						if(matchingFlag == null){
							matchingFlag = flag;
							oneMatchingFlag = true;
						}
						else{
							tabCompletes.add(flag);
							tabCompletes.add(matchingFlag);
							oneMatchingFlag = false;
						}
					}
				}
			}
			if(oneMatchingFlag) return getFlagCompletions(matchingFlag, lastArg);
			return tabCompletes;
		}
		return null;
	}

	@Override public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	usage: /hm spawn [n:name] [s:speed] [j:jump] [h:health] [c:color] [v:variant] [t:style] [o:owner] [r:tamer]
		//hm spawn n:fat c:white t:white s:50 j:20 h:60
		if(sender instanceof Player == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players");
			COMMAND_SUCCESS = false;
			return true;
		}

		String name = null;
		AnimalTamer tamer = null;
		OfflinePlayer owner = null;
		EntityType variant = EntityType.HORSE;
		Color color = null;
		Style style = null;
		double jump=0, speed=0, health=0;
		Boolean baby = null;

		for(String input : args){
			String arg = input;
			int sep = arg.indexOf(':');
			if(sep == -1){
				sender.sendMessage(ChatColor.RED+"Invalid arguments (perhaps missing ':'?)");
				COMMAND_SUCCESS = false;
				return false;
			}
			String postSep = arg.substring(sep+1);
			if(postSep.isBlank()){
				sender.sendMessage(ChatColor.RED+"Missing value after '"+input.substring(0, sep+1)+"'");
				COMMAND_SUCCESS = false;
				return false;
			}
			arg = arg.toUpperCase();
			if(arg.startsWith("N")) name = postSep;
			else if(arg.startsWith("V") || arg.startsWith("TYPE:")){
				try{variant = EntityType.valueOf(postSep.toUpperCase());}
				catch(IllegalArgumentException ex){
					sender.sendMessage(ChatColor.RED+"Invalid variant \""+ChatColor.GRAY+postSep
						+ChatColor.RED+"\"\n"+ChatColor.GRAY+"Possible values: "+claimableTypes
						/*Arrays.asList(Variant.values())*/);
					COMMAND_SUCCESS = false;
					return true;
				}
			}
			else if(arg.startsWith("C")){
				try{color = Color.valueOf(postSep.toUpperCase());}
				catch(IllegalArgumentException ex){
					sender.sendMessage(ChatColor.RED+"Invalid color \""+ChatColor.GRAY+postSep
							+ChatColor.RED+"\"\n"+ChatColor.GRAY+"Possible values: "+Arrays.asList(Color.values()));
					COMMAND_SUCCESS = false;
					return true;
				}
			}
			else if(arg.startsWith("STYLE:")){
				try{style = Style.valueOf(postSep.toUpperCase());}
				catch(IllegalArgumentException ex){
					sender.sendMessage(ChatColor.RED+"Invalid style \""+ChatColor.GRAY+postSep
							+ChatColor.RED+"\"\n"+ChatColor.GRAY+"Possible values: "+Arrays.asList(Style.values()));
					COMMAND_SUCCESS = false;
					return true;
				}
			}
			else if(arg.startsWith("J")){
				try{jump = Double.parseDouble(postSep);}
				catch(NumberFormatException ex){
					sender.sendMessage(ChatColor.RED+"Invalid jump, only accepts number values");
					COMMAND_SUCCESS = false;
					return true;
				}
			}
			else if(arg.startsWith("S")){
				try{speed = Double.parseDouble(postSep);}
				catch(NumberFormatException ex){
					sender.sendMessage(ChatColor.RED+"Invalid speed, only accepts number values");
					COMMAND_SUCCESS = false;
					return true;
				}
			}
			else if(arg.startsWith("H")){
				try{health = Double.parseDouble(postSep);}
				catch(NumberFormatException ex){
					sender.sendMessage(ChatColor.RED+"Invalid health, only accepts number values");
					COMMAND_SUCCESS = false;
					return true;
				}
			}
			else if(arg.startsWith("R") || arg.startsWith("TAMER")){
				tamer = plugin.getServer().getOfflinePlayer(postSep);
				if(tamer == null){
					sender.sendMessage(ChatColor.RED+"Unknown player '"+tamer+"' in tamer parameter");
					COMMAND_SUCCESS = false;
					return true;
				}
			}
			else if(arg.startsWith("O") || arg.startsWith("OWNER")){
				owner = plugin.getServer().getOfflinePlayer(postSep);
				if(owner == null){
					sender.sendMessage(ChatColor.RED+"Unknown player '"+owner+"' in owner parameter");
					COMMAND_SUCCESS = false;
					return true;
				}
			}
			else if(arg.startsWith("B")) baby = Boolean.parseBoolean(postSep);
			//else if(arg.startsWith("TAME")) tamed = postSep.equals("TRUE") || postSep.equals("YES");
		}

		Player p = (Player) sender;
		AbstractHorse horse = (AbstractHorse) p.getWorld().spawnEntity(p.getLocation(), variant);
		if(horse == null){
			sender.sendMessage(ChatColor.RED+"Failed to spawn horse (blocked by plugin or world settings)");
			COMMAND_SUCCESS = false;
			return true;
		}

		if(variant == EntityType.HORSE){
			if(color != null) ((Horse)horse).setColor(color);
			if(style != null) ((Horse)horse).setStyle(style);
		}
		if(jump != 0) horse.setJumpStrength(HorseLibrary.denormalizeJump(jump));
		if(speed != 0)
			horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(HorseLibrary.denormalizeSpeed(speed));
		if(health != 0){
			horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
			horse.setHealth(health);
		}
		//if(tamed) horse.setTamed(true);
		if(baby != null){if(baby) horse.setBaby(); else horse.setAdult();}
		if(name != null) horse.setCustomName(name);
		if(tamer != null){horse.setTamed(true); horse.setOwner(tamer);}
		if(owner != null) plugin.addClaimedHorse(owner.getUniqueId(), horse);

		sender.sendMessage(ChatColor.GREEN+"Successfully spawned your horse!");
		COMMAND_SUCCESS = true;
		return true;
	}
}