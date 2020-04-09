package net.evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import net.evmodder.HorseOwners.HorseLibrary;

public class CommandCopyHorse extends HorseCommand {
	boolean safeTeleports;

	public CommandCopyHorse(){
		safeTeleports = plugin.getConfig().getBoolean("teleport-only-if-safe");
	}

	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length > 0 && sender instanceof Player){
			String arg = String.join(" ", args).toLowerCase();
			final List<String> tabCompletes = new ArrayList<String>();
			byte shown = 0;
			for(String horseName : sender.hasPermission("horseowners.override")
					? plugin.getAllHorses()
					: plugin.getHorseOwners().get(((Player)sender).getUniqueId())){
				if(horseName.startsWith(arg)){
					tabCompletes.add(horseName);
					if(++shown == 20) break;
				}
			}
			return tabCompletes;
		}
		return null;
	}

	@Override public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	usage: /hm copy [horse]
		if(sender instanceof Player == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players");
			COMMAND_SUCCESS = false;
			return true;
		}

		Player p = (Player)sender;
		String target = StringUtils.join(args, ' ');
		AbstractHorse horse;

		if(safeTeleports && !HorseLibrary.safeForHorses(p.getLocation())){
			p.sendMessage(ChatColor.RED+
				"Unable to spawn horse - Please move to a more open area to prevent risk of horse suffocation");
			COMMAND_SUCCESS = false;
			return true;
		}

		if(!plugin.isClaimedHorse(target)){
			sender.sendMessage(ChatColor.RED+"Unknown horse '"+ChatColor.GRAY+target+ChatColor.RED+"'");
//			sender.sendMessage(ChatColor.RED+"Unclaimed horses cannot be copied via command, you must first use /claimhorse");
			COMMAND_SUCCESS = false;
			return false;
		}
		else if(!plugin.canAccess(p, target)){
			p.sendMessage(ChatColor.RED+"You cannot copy horses which you do not own");
			COMMAND_SUCCESS = false;
			return true;
		}
		Entity e = plugin.findClaimedHorse(target, null);
		horse = (e != null && e instanceof AbstractHorse) ? (AbstractHorse)e : null;
//		horse = HorseLibrary.findAnyHorse(target);
		if(horse == null){
			p.sendMessage(ChatColor.RED+"Unable to find your horse! Perhaps its location was unloaded?");
			COMMAND_SUCCESS = false;
			return true;
		}

		AbstractHorse newHorse = (AbstractHorse) p.getWorld().spawnEntity(p.getLocation(), horse.getType());
		newHorse.setCustomName("Copy of "+horse.getCustomName());
		if(horse instanceof Horse){
			((Horse)newHorse).setColor(((Horse)horse).getColor());
			((Horse)newHorse).setStyle(((Horse)horse).getStyle());
		}
		// Jump, Speed, Health
		newHorse.setJumpStrength(horse.getJumpStrength());
		newHorse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue());
		newHorse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
		// Tamed status
		newHorse.setTamed(horse.isTamed());
		if(horse.getOwner() != null) newHorse.setOwner(horse.getOwner());//Tamer
		// Baby status
		if(horse.isAdult()) newHorse.setAdult(); else newHorse.setBaby();
//		newHorse.setTicksLived(horse.getTicksLived());
//		newHorse.setAge(horse.getAge());
		newHorse.setAgeLock(horse.getAgeLock());
		// General entity data
		newHorse.setAI(horse.hasAI());//LivingEntity
		newHorse.setCollidable(horse.isCollidable());//LivingEntity
		newHorse.setCustomNameVisible(horse.isCustomNameVisible());//Entity
		newHorse.setDomestication(horse.getDomestication());//AbstractHorse
		newHorse.setGliding(horse.isGliding());//LivingEntity
		newHorse.setGlowing(horse.isGlowing());//Entity
		newHorse.setGravity(horse.hasGravity());//Entity
		newHorse.setInvulnerable(horse.isInvulnerable());//Entity
//		newHorse.setSeed(horse.getSeed());//Lootable
//		newHorse.setLootTable(horse.getLootTable());//Lootable
		newHorse.setSilent(horse.isSilent());//Entity
		newHorse.setInvulnerable(horse.isInvulnerable());//Entity
		newHorse.setNoDamageTicks(horse.getNoDamageTicks());//LivingEntity
		newHorse.setOp(horse.isOp());//ServerOperator
		newHorse.setRemoveWhenFarAway(horse.getRemoveWhenFarAway());//LivingEntity
		newHorse.setSilent(horse.isSilent());//Entity
		newHorse.addPotionEffects(horse.getActivePotionEffects());// Might not preserve "hidden" status for particles
		newHorse.getInventory().setContents(horse.getInventory().getContents());// Risky for duping reasons.

		// Metadata objects:
		String mother = HorseLibrary.getMother(horse); if(mother != null) HorseLibrary.setMother(newHorse, mother);
		String father = HorseLibrary.getFather(horse); if(father != null) HorseLibrary.setFather(newHorse, father);
		String dna = HorseLibrary.getDNA(horse, null); if(dna != null) HorseLibrary.setDNA(newHorse, dna);
		Long timeBorn = HorseLibrary.getTimeBorn(horse); if(timeBorn != null) HorseLibrary.setTimeBorn(newHorse, timeBorn);
		for(MetadataValue claimEvt : horse.getMetadata("claimed_by")) newHorse.setMetadata("claimed_by", claimEvt);

		// Not copied:
//		HorseLibrary.setSpawnReason(newHorse, HorseLibrary.getSpawnReason(horse));
//		newHorse.getScoreboardTags().addAll(horse.getScoreboardTags());

		sender.sendMessage(ChatColor.GREEN+"Successfully copied the horse!");
		COMMAND_SUCCESS = true;
		return true;
	}
}