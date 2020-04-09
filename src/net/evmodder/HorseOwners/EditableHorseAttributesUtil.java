package net.evmodder.HorseOwners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Style;

public class EditableHorseAttributesUtil{
	final HashMap<String, List<String>> flags;
	final HashSet<String> unusableFlags;
	//final String[] variants = new String[]{"HORSE", "DONKEY", "MULE", "LLAMA", "TRADER_LLAMA", "SKELETON_HORSE", "ZOMBIE_HORSE"};
	final Set<EntityType> claimableTypes;
	final HorseManager plugin;

	public EditableHorseAttributesUtil(HorseManager pl){
		plugin = pl;
		claimableTypes = plugin.getClaimableHorseTypes();
		flags = new HashMap<String, List<String>>();
		unusableFlags = new HashSet<String>();
		flags.put("name:", null);
		flags.put("speed:", null);
		flags.put("jump:", null);
		flags.put("health:", null);
		flags.put("owner:", null);
		flags.put("tamer:", null);
		//flags.put("variant:", Arrays.stream(Variant.values()).map(v -> "variant:"+v).collect(Collectors.toList()));
		flags.put("variant:", claimableTypes.stream().map(v -> "variant:"+v).collect(Collectors.toList()));
		flags.put("color:", Arrays.stream(Color.values()).map(c -> "color:"+c).collect(Collectors.toList()));
		flags.put("style:", Arrays.stream(Style.values()).map(s -> "style:"+s).collect(Collectors.toList()));
		flags.put("baby:", Arrays.asList("baby:true", "baby:false"));
	}

	public boolean addUnusableFlag(String flag){
		return unusableFlags.add(flag) && unusableFlags.add(flag+":");}
	public boolean removeUnusableFlag(String flag){
		return unusableFlags.remove(flag) && unusableFlags.remove(flag+":");}

	List<String> getFlagCompletions(String flag, String arg){
		final List<String> tabCompletes = new ArrayList<String>();
		final List<String> flagVals = flags.get(flag);
		if(flagVals != null) for(String flagVal : flagVals){
			if(flagVal.toLowerCase().startsWith(arg)) tabCompletes.add(flagVal);
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
	public List<String> getTabCompletions(CommandSender sender, String[] args){
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
				if(unusableFlags.contains(flag)) continue;
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

	public class HorseAttributes{
		public String name = null;
		public AnimalTamer tamer = null;
		public OfflinePlayer owner = null;
		public EntityType variant = EntityType.HORSE;
		public Color color = null;
		public Style style = null;
		public Double jump = null, speed = null, health = null;
		public Boolean baby = null;
		public boolean success = false, parseError = false;
	};
	@SuppressWarnings("deprecation")
	public HorseAttributes parseAttributes(CommandSender sender, String[] args){
		HorseAttributes attributes = new HorseAttributes();
		for(String input : args){
			String arg = input;
			int sep = arg.indexOf(':');
			if(sep == -1){
				if(sender != null) sender.sendMessage(ChatColor.RED+"Invalid arguments (perhaps missing ':'?)");
				attributes.parseError = true;
				return attributes;
			}
			String postSep = arg.substring(sep+1);
			if(postSep.isBlank()){
				if(sender != null) sender.sendMessage(ChatColor.RED+"Missing value after '"+input.substring(0, sep+1)+"'");
				attributes.parseError = true;
				return attributes;
			}
			arg = arg.toUpperCase();
			if(arg.startsWith("N")){
				/*if(unusableFlags.contains("name")){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Name cannot be modified for this entity");
					attributes.parseError = true;
					return attributes;
				}*/
				attributes.name = postSep;
			}
			else if(arg.startsWith("V") || arg.startsWith("TYPE:")){
				if(unusableFlags.contains("variant")){
					if(sender != null) sender.sendMessage(ChatColor.RED+"HorseType cannot be modified for this entity");
					attributes.parseError = true;
					return attributes;
				}
				try{attributes.variant = EntityType.valueOf(postSep.toUpperCase());}
				catch(IllegalArgumentException ex){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Invalid variant \""+ChatColor.GRAY+postSep
						+ChatColor.RED+"\"\n"+ChatColor.GRAY+"Possible values: "+claimableTypes
						/*Arrays.asList(Variant.values())*/);
					return attributes;
				}
			}
			else if(arg.startsWith("C")){
				if(unusableFlags.contains("color")){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Horse color cannot be modified for this entity");
					attributes.parseError = true;
					return attributes;
				}
				try{attributes.color = Color.valueOf(postSep.toUpperCase());}
				catch(IllegalArgumentException ex){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Invalid color \""+ChatColor.GRAY+postSep
							+ChatColor.RED+"\"\n"+ChatColor.GRAY+"Possible values: "+Arrays.asList(Color.values()));
					return attributes;
				}
			}
			else if(arg.startsWith("STYLE:") || arg.startsWith("T:")){
				if(unusableFlags.contains("style")){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Horse pattern cannot be modified for this entity");
					attributes.parseError = true;
					return attributes;
				}
				try{attributes.style = Style.valueOf(postSep.toUpperCase());}
				catch(IllegalArgumentException ex){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Invalid style \""+ChatColor.GRAY+postSep
							+ChatColor.RED+"\"\n"+ChatColor.GRAY+"Possible values: "+Arrays.asList(Style.values()));
					return attributes;
				}
			}
			else if(arg.startsWith("J")){
				/*if(unusableFlags.contains("jump")){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Jump cannot be modified for this entity");
					attributes.parseError = true;
					return attributes;
				}*/
				try{attributes.jump = Double.parseDouble(postSep);}
				catch(NumberFormatException ex){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Invalid jump, only accepts number values");
					return attributes;
				}
			}
			else if(arg.startsWith("S")){
				/*if(unusableFlags.contains("speed")){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Speed cannot be modified for this entity");
					attributes.parseError = true;
					return attributes;
				}*/
				try{attributes.speed = Double.parseDouble(postSep);}
				catch(NumberFormatException ex){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Invalid speed, only accepts number values");
					return attributes;
				}
			}
			else if(arg.startsWith("H")){
				/*if(unusableFlags.contains("health")){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Health cannot be modified for this entity");
					attributes.parseError = true;
					return attributes;
				}*/
				try{attributes.health = Double.parseDouble(postSep);}
				catch(NumberFormatException ex){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Invalid health, only accepts number values");
					return attributes;
				}
			}
			else if(arg.startsWith("R") || arg.startsWith("TAMER")){
				/*if(unusableFlags.contains("tamer")){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Tamer cannot be modified for this entity");
					attributes.parseError = true;
					return attributes;
				}*/
				attributes.tamer = plugin.getServer().getOfflinePlayer(postSep);
				if(attributes.tamer == null){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Unknown player '"+attributes.tamer+"' in tamer parameter");
					return attributes;
				}
			}
			else if(arg.startsWith("O") || arg.startsWith("OWNER")){
				/*if(unusableFlags.contains("owner")){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Owner cannot be modified for this entity");
					attributes.parseError = true;
					return attributes;
				}*/
				attributes.owner = plugin.getServer().getOfflinePlayer(postSep);
				if(attributes.owner == null){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Unknown player '"+attributes.owner+"' in owner parameter");
					return attributes;
				}
			}
			else if(arg.startsWith("B")){
				/*if(unusableFlags.contains("baby")){
					if(sender != null) sender.sendMessage(ChatColor.RED+"Baby/Adult status cannot be modified for this entity");
					attributes.parseError = true;
					return attributes;
				}*/
				attributes.baby = Boolean.parseBoolean(postSep);
			}
			//else if(arg.startsWith("TAME")) tamed = postSep.equals("TRUE") || postSep.equals("YES");
		}
		attributes.success = true;
		return attributes;
	}

	public void applyAttributes(Entity horse, HorseAttributes attributes){
		if(attributes.variant == EntityType.HORSE){
			if(attributes.color != null) ((Horse)horse).setColor(attributes.color);
			if(attributes.style != null) ((Horse)horse).setStyle(attributes.style);
		}
		if(horse instanceof Attributable){
			if(attributes.speed != null)
				((AbstractHorse)horse).getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(HorseLibrary.denormalizeSpeed(attributes.speed));
			if(attributes.health != null && horse instanceof Damageable){
				((Attributable)horse).getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(attributes.health);
				((Damageable)horse).setHealth(attributes.health);
			}
			if(horse instanceof AbstractHorse){
				if(attributes.jump != null) ((AbstractHorse)horse).setJumpStrength(HorseLibrary.denormalizeJump(attributes.jump));
			}
		}
		//if(tamed) horse.setTamed(true);
		if(attributes.baby != null && horse instanceof Ageable){
			if(attributes.baby.booleanValue()) ((Ageable)horse).setBaby(); else ((Ageable)horse).setAdult();
		}
		if(attributes.name != null) horse.setCustomName(attributes.name);
		if(attributes.tamer != null && horse instanceof Tameable){
			((Tameable)horse).setTamed(true); ((Tameable)horse).setOwner(attributes.tamer);
		}
		if(attributes.owner != null) plugin.addClaimedHorse(attributes.owner.getUniqueId(), horse);
	}
}