package evmodder.HorseOwners.listeners;

import java.util.Random;
import java.util.UUID;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Horse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import EvLib2.FileIO;
import evmodder.HorseOwners.HorseLibrary;
import evmodder.HorseOwners.HorseManager;

public class BreedListener implements Listener{
	private HorseManager plugin;
	enum ParentType{ MOTHER, FATHER, RANDOM };
	final boolean saveLineage, nameAtBirth, claimAtBirth;
	ParentType ownerAtBirth;
	final double MAX_JUMP, MAX_SPEED, MAX_HEALTH;
	final String[] horseNameList;

	public BreedListener(){
		plugin = HorseManager.getPlugin();
		saveLineage = plugin.getConfig().getBoolean("save-horse-lineage", true);
		nameAtBirth = plugin.getConfig().getBoolean("name-at-birth", false);
		claimAtBirth = plugin.getConfig().getBoolean("claim-at-birth", true);
		try{ownerAtBirth = ParentType.valueOf(plugin.getConfig()
				.getString("owner-at-birth-is-owner-of", "RANDOM").toUpperCase());}
		catch(IllegalArgumentException ex){ownerAtBirth = ParentType.RANDOM;}
		horseNameList = FileIO.loadResource(plugin, "horse-names.txt").split("\n");

		MAX_JUMP = HorseLibrary.denormalizeJump(plugin.getConfig().getDouble("max-jump", 5.29));
		MAX_SPEED = HorseLibrary.denormalizeSpeed(plugin.getConfig().getDouble("max-speed", 14.5));
		MAX_HEALTH = plugin.getConfig().getDouble("max-health", 30);
	}

	@EventHandler(priority = EventPriority.MONITOR)// In case event is cancelled
	public void onBreed(EntityBreedEvent evt){
		if(evt.getEntity() instanceof Horse/* && ((Horse)evt.getEntity()).getVariant() == Variant.HORSE*/){
			Horse h = (Horse)evt.getEntity();
			Random rand = new Random();

			//jump
			if(h.getJumpStrength() > MAX_JUMP) h.setJumpStrength(MAX_JUMP);

			//speed
			double speed = HorseLibrary.speedCalc.getHorseSpeed(h);
			speed *= ((rand.nextInt(81)+50)/100.0);//removes up to 50% of the speed and adds up to 30% of the speed
			if(speed > MAX_SPEED) speed = MAX_SPEED;
			HorseLibrary.speedCalc.setHorseSpeed(h, speed);

			//health
			double health = h.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			health *= ((rand.nextInt(71)+50)/100.0);//removes up to 50% of the health and adds up to 20% of the health
			if(health > MAX_HEALTH) health = MAX_HEALTH;
			HorseLibrary.setMaxHealth(h, health);

			if(nameAtBirth){
				h.setCustomName(horseNameList[rand.nextInt(horseNameList.length)]);

				if(claimAtBirth){
					UUID mothersOwner = evt.getMother().getCustomName() == null ? null
							: plugin.getHorseOwner(evt.getMother().getCustomName());
					UUID fathersOwner = evt.getFather().getCustomName() == null ? null
							: plugin.getHorseOwner(evt.getFather().getCustomName());

					switch(ownerAtBirth){
						case MOTHER:
							if(mothersOwner != null) plugin.addHorse(mothersOwner, h);
							else if(fathersOwner != null) plugin.addHorse(fathersOwner, h);
							break;
						case FATHER:
							if(fathersOwner != null) plugin.addHorse(fathersOwner, h);
							else if(mothersOwner != null) plugin.addHorse(mothersOwner, h);
							break;
						case RANDOM:
							if(mothersOwner == null && fathersOwner == null);
							else if(mothersOwner != null) plugin.addHorse(mothersOwner, h);
							else if(fathersOwner != null) plugin.addHorse(fathersOwner, h);
							else plugin.addHorse(rand.nextBoolean() ? mothersOwner : fathersOwner, h);
							break;
					}
				}
			}

			if(saveLineage){
				//Save mother and father in meta, even if names are null
				String mother = evt.getMother().getCustomName();
				String father = evt.getFather().getCustomName();

				if(mother != null) HorseLibrary.setMother(h, mother);
				if(father != null) HorseLibrary.setFather(h, father);
				plugin.updateData(h);
			}
		}
	}
}